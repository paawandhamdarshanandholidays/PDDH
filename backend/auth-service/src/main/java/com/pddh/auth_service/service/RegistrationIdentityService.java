package com.pddh.auth_service.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.pddh.auth_service.dto.UsernameAvailabilityResponse;
import com.pddh.auth_service.repository.UserRepository;
import com.pddh.auth_service.security.UserRole;

@Service
public class RegistrationIdentityService {
    private final UserRepository userRepository;

    public RegistrationIdentityService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UsernameAvailabilityResponse checkAvailability(String email, UserRole role, String requestedUsername, String mobile) {
        String normalizedEmail = normalizeEmail(email);
        String username = usernameFor(normalizedEmail, requestedUsername, role, null);
        boolean emailAvailable = !userRepository.existsByRoleIgnoreCaseAndEmailIgnoreCase(
                role.name(), normalizedEmail);
        boolean mobileAvailable = mobile == null || mobile.isBlank()
                || !userRepository.existsByRoleIgnoreCaseAndMobileIgnoreCase(role.name(), mobile.trim());
        return new UsernameAvailabilityResponse(username, true, emailAvailable, mobileAvailable);
    }

    public String usernameFor(String email, String requestedUsername, UserRole role, Long excludedId) {
        String normalizedEmail = normalizeEmail(email);
        String baseUsername = ProfileRegistrationValues.usernameFromEmail(normalizedEmail);
        if (isEmailBasedUsername(requestedUsername, baseUsername)
                && !usernameExists(role, requestedUsername.trim(), excludedId)) {
            return requestedUsername.trim();
        }
        return uniqueUsername(normalizedEmail, role, excludedId);
    }

    public void validateUnique(
            UserRole role, String email, String username, String mobile, Long excludedId) {
        if (emailExists(role, email, excludedId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered for this role");
        }
        if (usernameExists(role, username, excludedId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username is already registered for this role");
        }
        if (mobile != null && !mobile.isBlank() && mobileExists(role, mobile, excludedId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Mobile number is already registered for this role");
        }
    }

    private String uniqueUsername(String email, UserRole role, Long excludedId) {
        String baseUsername = ProfileRegistrationValues.usernameFromEmail(email);
        String candidate = baseUsername;
        int suffix = 1;
        while (usernameExists(role, candidate, excludedId)) {
            candidate = baseUsername + suffix++;
        }
        return candidate;
    }

    private boolean emailExists(UserRole role, String email, Long excludedId) {
        return excludedId == null
                ? userRepository.existsByRoleIgnoreCaseAndEmailIgnoreCase(role.name(), email)
                : userRepository.existsByRoleIgnoreCaseAndEmailIgnoreCaseAndIdNot(role.name(), email, excludedId);
    }

    private boolean usernameExists(UserRole role, String username, Long excludedId) {
        return excludedId == null
                ? userRepository.existsByRoleIgnoreCaseAndUsernameIgnoreCase(role.name(), username)
                : userRepository.existsByRoleIgnoreCaseAndUsernameIgnoreCaseAndIdNot(role.name(), username, excludedId);
    }

    private boolean mobileExists(UserRole role, String mobile, Long excludedId) {
        return excludedId == null
                ? userRepository.existsByRoleIgnoreCaseAndMobileIgnoreCase(role.name(), mobile)
                : userRepository.existsByRoleIgnoreCaseAndMobileIgnoreCaseAndIdNot(role.name(), mobile, excludedId);
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A valid email address is required");
        }
        return email.trim();
    }

    private boolean isEmailBasedUsername(String username, String baseUsername) {
        if (username == null || username.isBlank()) {
            return false;
        }
        String candidate = username.trim();
        if (candidate.equalsIgnoreCase(baseUsername)) {
            return true;
        }
        if (!candidate.regionMatches(true, 0, baseUsername, 0, baseUsername.length())
                || candidate.length() == baseUsername.length()) {
            return false;
        }
        return candidate.substring(baseUsername.length()).chars().allMatch(Character::isDigit);
    }
}
