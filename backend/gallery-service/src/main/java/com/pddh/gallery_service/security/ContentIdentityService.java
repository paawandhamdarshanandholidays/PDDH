package com.pddh.gallery_service.security;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;

@Service
public class ContentIdentityService {
    private static final Set<String> ROLES = Set.of("CEO", "ADMIN", "MANAGER", "STAFF", "CUSTOMER");
    private final SecretKey signingKey;

    public ContentIdentityService(@Value("${app.jwt.secret}") String secret) {
        signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String requireRole(HttpServletRequest request) {
        return parseClaims(request).get("role", String.class).trim().toUpperCase(Locale.ROOT);
    }

    public String requireSubject(HttpServletRequest request) {
        return parseClaims(request).getSubject();
    }

    private Claims parseClaims(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required");
        }
        try {
            Claims claims = Jwts.parser().verifyWith(signingKey).build()
                    .parseSignedClaims(authorization.substring(7)).getPayload();
            String role = claims.get("role", String.class);
            if (role == null || claims.getSubject() == null
                    || !ROLES.contains(role.trim().toUpperCase(Locale.ROOT))) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token identity is incomplete");
            }
            return claims;
        } catch (JwtException | IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired access token");
        }
    }
}
