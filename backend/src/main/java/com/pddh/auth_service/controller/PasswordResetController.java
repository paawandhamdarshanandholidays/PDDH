package com.pddh.auth_service.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pddh.api_common.dto.ApiMessageResponse;
import com.pddh.auth_service.dto.PasswordChangeRequest;
import com.pddh.auth_service.dto.PasswordResetApprovalRequest;
import com.pddh.auth_service.dto.PasswordResetResponse;
import com.pddh.auth_service.entities.User;
import com.pddh.auth_service.security.CurrentUserService;
import com.pddh.auth_service.service.PasswordResetService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/users")
public class PasswordResetController {
    private final CurrentUserService currentUserService;
    private final PasswordResetService passwordResetService;

    public PasswordResetController(
            CurrentUserService currentUserService, PasswordResetService passwordResetService) {
        this.currentUserService = currentUserService;
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/me/password")
    public ResponseEntity<ApiMessageResponse> changeOwnCustomerPassword(
            @RequestBody PasswordChangeRequest request, HttpServletRequest servletRequest) {
        User actor = currentUserService.requireUser(servletRequest);
        return ResponseEntity.ok(new ApiMessageResponse(passwordResetService.changeOwnCustomerPassword(actor, request)));
    }

    @PostMapping("/password-reset-requests")
    public ResponseEntity<PasswordResetResponse> requestRolePasswordReset(
            @RequestBody PasswordResetApprovalRequest request, HttpServletRequest servletRequest) {
        User actor = currentUserService.requireUser(servletRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(passwordResetService.requestRolePasswordReset(actor, request));
    }

    @GetMapping("/password-reset-targets")
    public List<com.pddh.auth_service.dto.UserProfileResponse> listPasswordResetTargets(
            HttpServletRequest servletRequest) {
        User actor = currentUserService.requireUser(servletRequest);
        return passwordResetService.listPasswordResetTargets(actor);
    }

    @GetMapping("/password-reset-requests")
    public List<PasswordResetResponse> listPendingPasswordResets(HttpServletRequest servletRequest) {
        User actor = currentUserService.requireUser(servletRequest);
        return passwordResetService.listPendingPasswordResets(actor);
    }

    @PatchMapping("/password-reset-requests/{id}")
    public PasswordResetResponse reviewPasswordReset(
            @PathVariable Long id,
            @RequestParam boolean approve,
            HttpServletRequest servletRequest) {
        User actor = currentUserService.requireUser(servletRequest);
        return passwordResetService.reviewPasswordReset(actor, id, approve);
    }
}
