package com.pddh.auth_service.dto;

import java.time.Instant;

public record PasswordResetResponse(
        String id,
        String targetUserId,
        String targetName,
        String targetUsername,
        String requesterName,
        Instant requestedAt,
        String status) {
}
