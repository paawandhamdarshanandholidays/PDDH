package com.pddh.auth_service.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pddh.auth_service.dto.RegisterRequest;
import com.pddh.auth_service.dto.UserProfilePageResponse;
import com.pddh.auth_service.dto.UserProfileResponse;
import com.pddh.auth_service.dto.UserProfileUpdateRequest;
import com.pddh.auth_service.entities.User;
import com.pddh.auth_service.security.CurrentUserService;
import com.pddh.auth_service.security.UserRole;
import com.pddh.auth_service.service.UserManagementService;
import com.pddh.auth_service.idempotency.IdempotencyService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final CurrentUserService currentUserService;
    private final UserManagementService userManagementService;
    private final IdempotencyService idempotencyService;

    public UserController(
            CurrentUserService currentUserService,
            UserManagementService userManagementService,
            IdempotencyService idempotencyService) {
        this.currentUserService = currentUserService;
        this.userManagementService = userManagementService;
        this.idempotencyService = idempotencyService;
    }

    @GetMapping
    public UserProfilePageResponse listUsers(
            @RequestParam UserRole role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            HttpServletRequest request) {
        UserRole actor = currentUserService.requireRole(request);
        return userManagementService.listManagedUsers(actor, role, page, size);
    }

    @PostMapping
    public ResponseEntity<UserProfileResponse> createUser(
            @RequestBody RegisterRequest user,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            HttpServletRequest request) {
        var currentUser = currentUserService.requireUser(request);
        UserRole actor = UserRole.parse(currentUser.getRole());
        UserProfileResponse response = idempotencyService.execute(
                currentUser.getUsername(), "users.create", idempotencyKey, user, UserProfileResponse.class,
                () -> userManagementService.createUser(actor, user));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(response);
    }

    @PatchMapping("/{id}")
    public UserProfileResponse updateUser(
            @PathVariable Long id,
            @RequestBody UserProfileUpdateRequest update,
            HttpServletRequest request) {
        UserRole actor = currentUserService.requireRole(request);
        return userManagementService.updateUser(actor, id, update);
    }

    @PatchMapping("/{id}/status")
    public UserProfileResponse setUserActive(
            @PathVariable Long id,
            @RequestBody StatusRequest status,
            HttpServletRequest request) {
        UserRole actor = currentUserService.requireRole(request);
        return userManagementService.setUserActive(actor, id, status.active());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id, HttpServletRequest request) {
        UserRole actor = currentUserService.requireRole(request);
        userManagementService.deleteUser(actor, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public UserProfileResponse getMyProfile(HttpServletRequest request) {
        User user = currentUserService.requireUser(request);
        return userManagementService.getProfile(user);
    }

    @PatchMapping("/me")
    public UserProfileResponse updateMyProfile(
            @RequestBody UserProfileUpdateRequest update,
            HttpServletRequest request) {
        User user = currentUserService.requireUser(request);
        return userManagementService.updateOwnProfile(user, update);
    }

    public record StatusRequest(boolean active) {
    }
}
