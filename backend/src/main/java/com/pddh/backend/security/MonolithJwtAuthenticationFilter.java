package com.pddh.backend.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class MonolithJwtAuthenticationFilter extends OncePerRequestFilter {
    private static final Logger LOGGER = LoggerFactory.getLogger(MonolithJwtAuthenticationFilter.class);
    private final SecretKey signingKey;

    public MonolithJwtAuthenticationFilter(@Value("${app.jwt.secret}") String secret) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            if (response.getStatus() == HttpServletResponse.SC_UNAUTHORIZED) {
                LOGGER.warn("Authentication rejected method={} path={} reason=missing_bearer_token",
                        request.getMethod(), request.getRequestURI());
            }
            return;
        }
        Claims claims;
        try {
            claims = Jwts.parser().verifyWith(signingKey).build()
                    .parseSignedClaims(authorization.substring(7)).getPayload();
        } catch (ExpiredJwtException exception) {
            rejectInvalidToken(request, response, "expired_token");
            return;
        } catch (JwtException | IllegalArgumentException exception) {
            rejectInvalidToken(request, response, "invalid_token");
            return;
        }
        String subject = claims.getSubject();
        String role;
        try {
            role = claims.get("role", String.class);
        } catch (JwtException | IllegalArgumentException exception) {
            rejectInvalidToken(request, response, "invalid_role_claim");
            return;
        }
        if (subject == null || subject.isBlank() || role == null || role.isBlank()) {
            rejectInvalidToken(request, response, "missing_identity_claims");
            return;
        }
        var authorities = java.util.List.of(new SimpleGrantedAuthority("ROLE_" + role));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(subject, null, authorities));
        filterChain.doFilter(request, response);
    }

    private void rejectInvalidToken(HttpServletRequest request, HttpServletResponse response, String reason)
            throws IOException {
        SecurityContextHolder.clearContext();
        LOGGER.warn("Authentication rejected method={} path={} reason={}",
                request.getMethod(), request.getRequestURI(), reason);
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired token");
    }
}
