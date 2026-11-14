package com.pddh.auth_service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.pddh.auth_service.entities.User;
import com.pddh.auth_service.security.JwtService;

class JwtServiceTest {

    @Test
    void generateToken_shouldCreateValidTokenThatExpiresAfterConfiguredLifetime() {
        JwtService jwtService = new JwtService("12345678901234567890123456789012");
        User user = new User();
        user.setUsername("john123");
        user.setRole("USER");

        String token = jwtService.generateToken(user);

        assertThat(token).isNotBlank();
        assertThat(token.split("\\.")).hasSize(3);

        var claims = jwtService.parseClaims(token);

        assertThat(claims.getExpiration()).isNotNull();
        assertThat(claims.getExpiration().getTime() - claims.getIssuedAt().getTime()).isEqualTo(3_600_000);
    }
}
