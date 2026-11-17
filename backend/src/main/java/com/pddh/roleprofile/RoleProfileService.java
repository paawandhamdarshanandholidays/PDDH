package com.pddh.roleprofile;

import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RoleProfileService {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoleProfileService.class);
    private final RoleProfileStore profiles;

    public RoleProfileService(RoleProfileStore profiles) {
        this.profiles = profiles;
    }

    public UserProfilePage list(String actor, String requestedRole, int page, int requestedSize) {
        String role = normalizeRole(requestedRole);
        if (!canView(actor, role)) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN,
                    "You cannot view these profiles");
        }
        if (page < 0 || requestedSize < 1) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Page must be non-negative and size positive");
        }
        int size = Math.min(requestedSize, 50);
        long offset = (long) page * size;
        String table = tableFor(role);
        LOGGER.debug("Listing role profiles role={} page={} size={}", role, page, size);
        long total = profiles.count(table);
        List<UserProfile> content = profiles.page(table, size, offset).stream()
                .map(profile -> new UserProfile(String.valueOf(profile.getIdentityId()), profile.getName(),
                        profile.getUsername(), profile.getEmail(),
                        profile.getMobile() == null ? "" : profile.getMobile(), profile.getRole(),
                        profile.isActive(), profile.getLastLoginAt()))
                .toList();
        return new UserProfilePage(content, page, size, total,
                total == 0 ? 0 : (int) Math.ceil((double) total / size));
    }

    private boolean canView(String actor, String target) {
        int actorRank = rank(normalizeRole(actor)), targetRank = rank(target);
        return actorRank <= targetRank;
    }

    private String normalizeRole(String role) {
        if (role == null) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Unsupported profile role");
        }
        return role.trim().toUpperCase(Locale.ROOT);
    }

    private String tableFor(String role) {
        return switch (role) {
            case "CUSTOMER" -> "users";
            case "ADMIN" -> "admin";
            case "MANAGER" -> "manager";
            case "CEO" -> "ceo";
            case "STAFF" -> "staff";
            default -> throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Unsupported profile role");
        };
    }

    private int rank(String role) {
        return switch (role) {
            case "CEO" -> 0;
            case "ADMIN" -> 1;
            case "MANAGER" -> 2;
            case "STAFF" -> 3;
            default -> 4;
        };
    }

    public record UserProfile(String id, String name, String username, String email, String mobile,
            String role, boolean active, java.time.Instant lastLoginAt) { }
    public record UserProfilePage(List<UserProfile> content, int page, int size, long totalElements, int totalPages) { }
}
