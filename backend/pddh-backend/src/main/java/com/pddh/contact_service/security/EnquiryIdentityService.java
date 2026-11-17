package com.pddh.contact_service.security;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;

@Service
public class EnquiryIdentityService {
    private static final Set<String> ROLES = Set.of("CEO", "ADMIN", "MANAGER", "STAFF");
    private final SecretKey signingKey;

    public EnquiryIdentityService(@Value("${app.jwt.secret}") String secret) {
        signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public void requireStaff(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required");
        }
        try {
            var claims = Jwts.parser().verifyWith(signingKey).build()
                    .parseSignedClaims(authorization.substring(7)).getPayload();
            String role = claims.get("role", String.class);
            if (claims.getSubject() == null || role == null
                    || !ROLES.contains(role.trim().toUpperCase(Locale.ROOT))) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Staff access is required");
            }
        } catch (JwtException | IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired access token");
        }
    }
}
