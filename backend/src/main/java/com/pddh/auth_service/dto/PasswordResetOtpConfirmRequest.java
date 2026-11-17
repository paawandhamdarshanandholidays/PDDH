package com.pddh.auth_service.dto;

public record PasswordResetOtpConfirmRequest(String email, String code, String newPassword) {
}
