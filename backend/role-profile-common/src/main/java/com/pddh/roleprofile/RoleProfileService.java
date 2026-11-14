package com.pddh.roleprofile;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RoleProfileService {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoleProfileService.class);
    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();
    private final RoleProfileRepository profiles;
    private final RoleProfileProperties properties;

    public RoleProfileService(RoleProfileRepository profiles, RoleProfileProperties properties) {
        this.profiles = profiles;
        this.properties = properties;
    }

    public UserProfilePage list(String actor, int page, int requestedSize) {
        if (!canView(actor, properties.role())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot view these profiles");
        }
        if (page < 0 || requestedSize < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Page must be non-negative and size positive");
        }
        int size = Math.min(requestedSize, 50);
        long offset = (long) page * size;
        LOGGER.debug("Listing role profiles role={} page={} size={}", properties.role(), page, size);
        long total = profiles.count();
        return new UserProfilePage(profiles.page(size, offset), page, size, total,
                total == 0 ? 0 : (int) Math.ceil((double) total / size));
    }

    public AuthenticationResponse authenticate(String identifier, String password) {
        if (identifier == null || identifier.isBlank() || password == null || password.isBlank()) return unauthorized();
        var profile = profiles.findForAuthentication(identifier.trim()).orElseThrow(this::unauthorizedException);
        if (!profile.active() || profile.passwordHash() == null
                || !PASSWORD_ENCODER.matches(password, profile.passwordHash())) {
            LOGGER.debug("Role authentication rejected role={} identifier={}", properties.role(), identifier);
            return unauthorized();
        }
        profiles.updateLastLogin(profile.identityId());
        LOGGER.debug("Role authentication succeeded role={} username={}", properties.role(), profile.username());
        return new AuthenticationResponse(profile.username(), profile.email(), properties.role());
    }

    public void sync(ProfileSyncEvent event) {
        if (!properties.role().equalsIgnoreCase(event.role())) {
            throw new IllegalArgumentException("Unexpected role on profile queue");
        }
        switch (event.operation()) {
            case "DELETE" -> profiles.delete(event.identityId());
            case "UPSERT" -> profiles.upsert(event);
            default -> throw new IllegalArgumentException("Unsupported profile operation");
        };
        LOGGER.debug("Synchronized role profile role={} operation={} identityId={}",
                properties.role(), event.operation(), event.identityId());
    }

    private AuthenticationResponse unauthorized() {
        throw unauthorizedException();
    }

    private ResponseStatusException unauthorizedException() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid role or credentials");
    }

    private boolean canView(String actor, String target) {
        int actorRank = rank(actor), targetRank = rank(target);
        return actorRank <= targetRank;
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
    public record AuthenticationRequest(String identifier, String password) { }
    public record AuthenticationResponse(String username, String email, String role) { }
}
