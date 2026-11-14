package com.pddh.roleprofile;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;

import javax.crypto.SecretKey;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;

public class RoleIdentityService {
    private static final Set<String> ROLES = Set.of("CEO", "ADMIN", "MANAGER", "STAFF", "CUSTOMER");
    private final SecretKey signingKey;

    public RoleIdentityService(String secret) {
        signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String requireRole(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required");
        }
        try {
            var claims = Jwts.parser().verifyWith(signingKey).build()
                    .parseSignedClaims(authorization.substring(7)).getPayload();
            String role = claims.get("role", String.class);
            if (claims.getSubject() == null || role == null) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token identity is incomplete");
            }
            role = role.trim().toUpperCase(Locale.ROOT);
            if (!ROLES.contains(role)) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token role is not supported");
            return role;
        } catch (JwtException | IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired access token");
        }
    }
}
