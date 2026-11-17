package com.pddh.auth_service.dto;

public record PasswordChangeRequest(String currentPassword, String newPassword) {
}
