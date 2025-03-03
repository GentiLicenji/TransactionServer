package com.sisal.transaction.server.config.auth;

import com.sisal.transaction.server.util.AuthUtil;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class HmacAuthenticationProvider implements AuthenticationProvider {

    private final ApiKeyProperties apiKeyProperties;

    public HmacAuthenticationProvider(ApiKeyProperties apiKeyProperties) {
        this.apiKeyProperties = apiKeyProperties;
    }

    @Override
    public Authentication authenticate(Authentication authentication)
            throws AuthenticationException {

        HmacAuthenticationToken token = (HmacAuthenticationToken) authentication;

            ApiKeyProperties.ApiClient client = apiKeyProperties.getClientByApiKey(token.getPrincipal().toString());
            if (client == null) {
                throw new BadCredentialsException("Authentication failed: Invalid API key");
            }

            String calculatedHmac = AuthUtil.calculateHmac(
                    token.getRequestDetails(),  // Contains method, path, body, etc.
                    client.getSecretKey()
            );

            if (!token.getCredentials().equals(calculatedHmac)) {
                throw new BadCredentialsException("Authentication failed: Invalid HMAC signature");
            }

            // Authentication successful, set security context
            List<GrantedAuthority> authorities = client.getRoles().stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .collect(Collectors.toList());

            // Return authenticated token
            return new HmacAuthenticationToken(client.getApiKey(), authorities);

    }

    @Override
    public boolean supports(Class<?> authentication) {
        return HmacAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
