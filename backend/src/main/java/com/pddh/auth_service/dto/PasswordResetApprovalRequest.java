package com.pddh.auth_service.dto;

public record PasswordResetApprovalRequest(Long targetUserId, String newPassword) {
}
