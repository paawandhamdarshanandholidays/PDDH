package com.pddh.auth_service.dto;

public record ProfileSyncEvent(
        String eventId, String operation, Long identityId, String role,
        String name, String username, String email, String passwordHash, String mobile, String gender, boolean active) {
}
