package com.pddh.auth_service.service;

import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pddh.auth_service.entities.User;
import com.pddh.roleprofile.RoleProfileStore;

@Service
public class ProfileSyncService {
    private static final Map<String, String> ROLE_TABLES = Map.of(
            "CUSTOMER", "users",
            "ADMIN", "admin",
            "MANAGER", "manager",
            "CEO", "ceo",
            "STAFF", "staff");

    private final RoleProfileStore profiles;
    private final UserDirectoryService userDirectory;

    public ProfileSyncService(RoleProfileStore profiles, UserDirectoryService userDirectory) {
        this.profiles = profiles;
        this.userDirectory = userDirectory;
    }

    @Transactional
    public void upsert(User user) {
        var event = toProfileEvent(user, "UPSERT");
        profiles.upsert(tableFor(user.getRole()), event);
        if ("CUSTOMER".equalsIgnoreCase(user.getRole())) {
            userDirectory.syncCustomer(event);
        }
    }

    @Transactional
    public void delete(User user) {
        profiles.delete(tableFor(user.getRole()), user.getId());
        if ("CUSTOMER".equalsIgnoreCase(user.getRole())) {
            userDirectory.deleteCustomer(user.getId());
        }
    }

    private String tableFor(String role) {
        String table = ROLE_TABLES.get(role == null ? "" : role.trim().toUpperCase(java.util.Locale.ROOT));
        if (table == null) {
            throw new IllegalArgumentException("Unsupported profile role");
        }
        return table;
    }

    private com.pddh.roleprofile.ProfileSyncEvent toProfileEvent(User user, String operation) {
        return new com.pddh.roleprofile.ProfileSyncEvent(null, operation, user.getId(), user.getRole(),
                user.getName(), user.getUsername(), user.getEmail(), user.getPassword(),
                user.getMobile(), user.getGender(), user.isActive());
    }
}
