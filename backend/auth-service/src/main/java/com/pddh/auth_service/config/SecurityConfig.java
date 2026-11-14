package com.pddh.auth_service.config;

import java.util.function.Supplier;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    public boolean isGatewayRequest(HttpServletRequest request) {
        return "true".equalsIgnoreCase(request.getHeader("X-Gateway-Request"));
    }

    AuthorizationDecision authorizeGatewayRequest(HttpServletRequest request) {
        return new AuthorizationDecision(isGatewayRequest(request));
    }

    AuthorizationDecision authorizeGatewayRequest(
            Supplier<? extends Authentication> authentication,
            RequestAuthorizationContext context) {
        return authorizeGatewayRequest(context.getRequest());
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers(
                                "/actuator/health", "/actuator/info", "/actuator/metrics/**", "/actuator/prometheus")
                            .permitAll()
                        .requestMatchers("/v3/api-docs", "/v3/api-docs/**")
                            .access(this::authorizeGatewayRequest)
                        .requestMatchers("/auth", "/auth/", "/auth/login", "/auth/register",
                                "/auth/register-ceo", "/auth/roles", "/auth/username-availability",
                                "/auth/password-reset/otp/request",
                                "/auth/password-reset/otp/confirm")
                            .access(this::authorizeGatewayRequest)
                        .requestMatchers("/api/users/**")
                            .access(this::authorizeGatewayRequest)
                        .anyRequest().denyAll())
                .build();
    }
}
