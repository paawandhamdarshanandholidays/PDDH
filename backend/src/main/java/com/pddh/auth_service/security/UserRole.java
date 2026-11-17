package com.pddh.auth_service.security;

import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public enum UserRole {
    CEO(0),
    ADMIN(1),
    MANAGER(2),
    STAFF(3),
    CUSTOMER(4);

    private final int rank;

    UserRole(int rank) {
        this.rank = rank;
    }

    public boolean canManage(UserRole target) {
        return rank < target.rank;
    }

    public boolean canCreate(UserRole target) {
        return this == ADMIN && target == ADMIN
                || this == CEO && target == CEO
                || canManage(target);
    }

    public static UserRole parse(String role) {
        if (role == null || role.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role is required");
        }
        try {
            return valueOf(role.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported role");
        }
    }
}
