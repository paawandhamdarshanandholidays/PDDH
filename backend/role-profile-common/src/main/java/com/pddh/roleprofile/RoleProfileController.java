package com.pddh.roleprofile;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
@RestController
public class RoleProfileController {
    private final RoleProfileService profiles;
    private final RoleIdentityService identities;

    public RoleProfileController(RoleProfileService profiles, RoleIdentityService identities) {
        this.profiles = profiles;
        this.identities = identities;
    }

    @GetMapping("${app.role.path}/profiles")
    public RoleProfileService.UserProfilePage list(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int requestedSize, HttpServletRequest request) {
        return profiles.list(identities.requireRole(request), page, requestedSize);
    }

    @PostMapping("/internal/authenticate")
    public RoleProfileService.AuthenticationResponse authenticate(
            @RequestBody RoleProfileService.AuthenticationRequest request) {
        if (request == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED, "Invalid role or credentials");
        }
        return profiles.authenticate(request.identifier(), request.password());
    }
}
