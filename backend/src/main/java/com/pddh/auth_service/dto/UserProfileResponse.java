package com.pddh.auth_service.dto;

import java.time.Instant;

public record UserProfileResponse(
        String id,
        String name,
        String username,
        String email,
        String mobile,
        String role,
        boolean active,
        Instant lastLoginAt) {
}
