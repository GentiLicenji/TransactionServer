package com.sisal.transaction.server.config;

import com.sisal.transaction.server.filter.AuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.SecurityContextHolderFilter;

/**
 * Security configuration class that sets up web security filters and authentication.
 * This class configures stateless authentication using custom filters and defines
 * security rules for different API endpoints.
 */
@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

    private final AuthenticationFilter authenticationFilter;

    @Autowired
    public WebSecurityConfig(AuthenticationFilter authenticationFilter) {
        this.authenticationFilter = authenticationFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf().disable()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .addFilterAt(authenticationFilter, SecurityContextHolderFilter.class)
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
