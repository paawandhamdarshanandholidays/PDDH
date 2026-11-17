package com.pddh.auth_service.service;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.pddh.auth_service.dto.RoleAuthenticationResponse;
import com.pddh.roleprofile.RoleProfileStore;

@Service
public class RoleAuthenticationClient {
    private static final Map<String, String> PROFILE_TABLES = Map.of(
            "CEO", "ceo",
            "ADMIN", "admin",
            "MANAGER", "manager",
            "STAFF", "staff",
            "CUSTOMER", "users");

    private final RoleProfileStore profiles;
    private final PasswordEncoder passwordEncoder;

    public RoleAuthenticationClient(RoleProfileStore profiles, PasswordEncoder passwordEncoder) {
        this.profiles = profiles;
        this.passwordEncoder = passwordEncoder;
    }

    public RoleAuthenticationResponse authenticate(String role, String identifier, String password) {
        String normalizedRole = role == null ? "" : role.trim().toUpperCase(java.util.Locale.ROOT);
        String table = PROFILE_TABLES.get(normalizedRole);
        if (table == null || identifier == null || identifier.isBlank()
                || password == null || password.isBlank()) {
            throw unauthorized();
        }
        RoleProfileStore.StoredProfile profile = profiles.findForAuthentication(table, identifier.trim())
                .orElseThrow(this::unauthorized);
        if (!profile.active() || profile.passwordHash() == null
                || !passwordEncoder.matches(password, profile.passwordHash())) {
            throw unauthorized();
        }
        profiles.updateLastLogin(table, profile.identityId());
        return new RoleAuthenticationResponse(profile.username(), profile.email(), normalizedRole);
    }

    private ResponseStatusException unauthorized() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid role or credentials");
    }
}
