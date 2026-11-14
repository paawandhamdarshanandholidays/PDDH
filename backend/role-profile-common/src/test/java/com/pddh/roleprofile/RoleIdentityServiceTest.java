package com.pddh.roleprofile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;

class RoleIdentityServiceTest {
    private static final String SECRET = "test-signing-secret-that-is-at-least-32-bytes-long";
    private RoleIdentityService identities;
    private HttpServletRequest request;
    private SecretKey key;

    @BeforeEach
    void setUp() {
        identities = new RoleIdentityService(SECRET);
        request = mock(HttpServletRequest.class);
        key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void returnsNormalizedRoleFromValidBearerToken() {
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token("  staff ", "person-1", null));
        assertThat(identities.requireRole(request)).isEqualTo("STAFF");
    }

    @Test
    void rejectsMissingBearerHeaderAndMalformedToken() {
        when(request.getHeader("Authorization")).thenReturn(null);
        assertStatus(HttpStatus.UNAUTHORIZED);
        when(request.getHeader("Authorization")).thenReturn("Basic token");
        assertStatus(HttpStatus.UNAUTHORIZED);
        when(request.getHeader("Authorization")).thenReturn("Bearer broken");
        assertStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void rejectsUnsupportedRoleAndExpiredToken() {
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token("ROOT", "person-1", null));
        assertStatus(HttpStatus.UNAUTHORIZED);
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token("STAFF", "person-1", new Date(1)));
        assertStatus(HttpStatus.UNAUTHORIZED);
    }

    private String token(String role, String subject, Date expiration) {
        var builder = Jwts.builder().subject(subject).claim("role", role);
        if (expiration != null) builder.expiration(expiration);
        return builder.signWith(key).compact();
    }

    private void assertStatus(HttpStatus status) {
        assertThatThrownBy(() -> identities.requireRole(request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(error -> ((ResponseStatusException) error).getStatusCode())
                .isEqualTo(status);
    }
}
