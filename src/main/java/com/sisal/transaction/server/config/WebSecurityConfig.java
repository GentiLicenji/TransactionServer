package com.sisal.transaction.server.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sisal.transaction.server.config.auth.HmacAuthenticationProvider;
import com.sisal.transaction.server.filter.AuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

/**
 * Security configuration class that sets up web security filters and authentication.
 * This class configures stateless authentication using custom filters and defines
 * security rules for different API endpoints.
 */
@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

    @Bean
    public AuthenticationManager authenticationManager(HmacAuthenticationProvider hmacAuthenticationProvider) {
        return new ProviderManager(hmacAuthenticationProvider);
    }

    @Bean
    public AuthenticationFilter authenticationFilter(AuthenticationManager authenticationManager, ObjectMapper objectMapper) {
        return new AuthenticationFilter(authenticationManager, objectMapper);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           HmacAuthenticationProvider hmacAuthenticationProvider,
                                           AuthenticationFilter authenticationFilter
    ) throws Exception {

        http.csrf().disable()
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .authorizeRequests()
                .antMatchers(AuthenticationFilter.EXCLUDED_PATHS).permitAll()// Exclude paths from Spring's authorization rules
                .anyRequest().authenticated()// All other APIs will need authorization including /api/**
                .and()
                .authenticationProvider(hmacAuthenticationProvider)// Register the provider
                .addFilterBefore(authenticationFilter, BasicAuthenticationFilter.class);// Inject filter into the security chain

        return http.build();
    }
}
