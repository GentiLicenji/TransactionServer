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
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
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
                .antMatchers("/api/**").permitAll()
                .antMatchers(EXCLUDED_PATHS).permitAll()
                .anyRequest().authenticated();

        return http.build();
    }


    /**
     * WhiteList Swagger UI resources and spring actuators.
     */
    public static final String[] EXCLUDED_PATHS = {
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/actuator/**",
            "/actuator/health",
            "/error",
            "/favicon.ico",
            "/auth/**",
            "/api/public/**",
            "/h2-console/**",
            "/login",
            "/register",
            "/health-check",
            "/metrics/**",
            "/webjars/**",
            "/css/**",
            "/js/**",
            "/images/**",
            "/assets/**"
    };
}
