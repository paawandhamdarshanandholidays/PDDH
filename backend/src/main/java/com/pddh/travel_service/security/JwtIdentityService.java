package com.pddh.travel_service.security;

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
public class JwtIdentityService {
    private static final Set<String> ROLES = Set.of("CEO", "ADMIN", "MANAGER", "STAFF", "CUSTOMER");
    private final SecretKey signingKey;

    public JwtIdentityService(@Value("${app.jwt.secret}") String secret) {
        signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public TravelIdentity requireIdentity(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required");
        }
        try {
            Claims claims = Jwts.parser().verifyWith(signingKey).build()
                    .parseSignedClaims(authorization.substring(7)).getPayload();
            String username = claims.getSubject();
            String role = claims.get("role", String.class);
            if (username == null || role == null) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token identity is incomplete");
            }
            role = role.trim().toUpperCase(Locale.ROOT);
            if (!ROLES.contains(role)) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token role is not supported");
            }
            return new TravelIdentity(username, claims.get("email", String.class), role);
        } catch (JwtException | IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired access token");
        }
    }
}
