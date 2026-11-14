package com.pddh.places_service.api_common.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class GatewayAccessFilter extends OncePerRequestFilter {
    private static final String GATEWAY_TOKEN_HEADER = "X-Internal-Gateway-Token";
    private final byte[] gatewayToken;

    public GatewayAccessFilter(@Value("${app.gateway.internal-token}") String gatewayToken) {
        if (gatewayToken == null || gatewayToken.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("GATEWAY_INTERNAL_TOKEN must be at least 32 bytes");
        }
        this.gatewayToken = gatewayToken.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().equals("/actuator")
                || request.getRequestURI().startsWith("/actuator/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String suppliedToken = request.getHeader(GATEWAY_TOKEN_HEADER);
        if (suppliedToken == null || !MessageDigest.isEqual(
                gatewayToken, suppliedToken.getBytes(StandardCharsets.UTF_8))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Requests must be routed through the API gateway");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
