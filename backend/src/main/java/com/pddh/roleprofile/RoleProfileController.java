package com.pddh.roleprofile;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Qualifier;

import jakarta.servlet.http.HttpServletRequest;
@RestController
public class RoleProfileController {
    private final RoleProfileService profiles;
    private final RoleIdentityService identities;

    public RoleProfileController(
            RoleProfileService profiles,
            @Qualifier("roleProfileIdentityService") RoleIdentityService identities) {
        this.profiles = profiles;
        this.identities = identities;
    }

    @GetMapping("/api/roles/{targetRole}/profiles")
    public RoleProfileService.UserProfilePage list(@PathVariable String targetRole,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int requestedSize, HttpServletRequest request) {
        return profiles.list(identities.requireRole(request), targetRole, page, requestedSize);
    }
}
