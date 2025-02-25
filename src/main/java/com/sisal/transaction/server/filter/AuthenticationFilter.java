package com.sisal.transaction.server.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sisal.transaction.server.config.ApiKeyProperties;
import com.sisal.transaction.server.exception.*;
import com.sisal.transaction.server.model.rest.ErrorResponse;
import com.sisal.transaction.server.util.AuthUtil;
import com.sisal.transaction.server.util.ErrorCode;
import com.sisal.transaction.server.util.logging.CustomRequestWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Authentication filter implementation based on Hash-based Message Authentication Code.
 */
public class AuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationFilter.class);

    private static final String HMAC_HEADER = "X-HMAC-SIGNATURE";
    private static final String API_KEY_HEADER = "X-API-KEY";
    private static final String TIMESTAMP_HEADER = "X-TIMESTAMP";
    private static final long MAX_TIMESTAMP_DIFF = 30 * 60 * 1000; // 30 minutes in milliseconds

    private final ApiKeyProperties apiKeyProperties;
    private final ObjectMapper objectMapper;

    public AuthenticationFilter(ApiKeyProperties apiKeyProperties, ObjectMapper objectMapper) {
        this.apiKeyProperties = apiKeyProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws IOException {
        try {

            // Create a wrapper to allow reading the request body multiple times
            CustomRequestWrapper requestWrapper = new CustomRequestWrapper(request);

            //Validate headers
            String apiKey = requestWrapper.getHeader(API_KEY_HEADER);
            String hmacSignature = requestWrapper.getHeader(HMAC_HEADER);
            String timestamp = requestWrapper.getHeader(TIMESTAMP_HEADER);
            if(!StringUtils.hasText(apiKey)){
                throw new AuthMissingHeaderException(API_KEY_HEADER+" is missing or empty!");
            }
            if(!StringUtils.hasText(hmacSignature)){
                throw new AuthMissingHeaderException(HMAC_HEADER+" is missing or empty!");
            }

            AuthUtil.validateTimestamp(timestamp, MAX_TIMESTAMP_DIFF);

            ApiKeyProperties.ApiClient client = apiKeyProperties.getClientByApiKey(apiKey);
            if (client == null) {
                throw new AuthException("Invalid API key");
            }

            //Validate HMAC signature
            String calculatedSignature = AuthUtil.calculateHmac(requestWrapper, timestamp, API_KEY_HEADER, client.getSecretKey());

            if (!hmacSignature.equals(calculatedSignature)) {
                throw new AuthSignatureException("Invalid HMAC signature");
            }

            // Authentication successful, set security context
            List<GrantedAuthority> authorities = client.getRoles().stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .collect(Collectors.toList());

            Authentication auth = new UsernamePasswordAuthenticationToken(client.getApiKey(),
                    null,// credentials (null for API key auth)
                    authorities);
            SecurityContextHolder.getContext().setAuthentication(auth);

            filterChain.doFilter(requestWrapper, response);

        } catch (AuthMissingHeaderException | AuthInvalidTimestampException | AuthTimestampExpiredException | AuthSignatureException | AuthException e) {
            logger.error(e.getMessage(), e);
            sendErrorResponse(response, HttpStatus.UNAUTHORIZED, e.getErrorCode(), e.getMessage());
        } catch (Exception exception) {
            logger.error(exception.getMessage(), exception);
            sendErrorResponse(response, HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.UNKOWN_ERROR, "Internal Unexpected Authentication error.Check Server logs for more details");
        }

    }

    /**
     * Sends REST error response for Auth failure.
     */
    private void sendErrorResponse(HttpServletResponse response, HttpStatus status, ErrorCode errorCode, String message) throws IOException {

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ErrorResponse errorResponse = new ErrorResponse()
                .httpErrorCode(status.toString())
                .errorCode(errorCode.getCode())
                .errorMessage(message);

        String jsonResponse = objectMapper.writeValueAsString(errorResponse);
        response.getWriter().write(jsonResponse);
    }
}