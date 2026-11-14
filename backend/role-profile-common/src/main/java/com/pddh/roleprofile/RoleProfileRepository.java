package com.pddh.roleprofile;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

@Repository
public class RoleProfileRepository {
    private final RoleProfileStore store;
    private final String table;

    public RoleProfileRepository(RoleProfileStore store, RoleProfileProperties properties) {
        this.store = store;
        this.table = properties.table();
    }

    public long count() {
        return store.count(table);
    }

    public List<RoleProfileService.UserProfile> page(int size, long offset) {
        return store.page(table, size, offset).stream()
                .map(profile -> new RoleProfileService.UserProfile(
                        String.valueOf(profile.getIdentityId()), profile.getName(), profile.getUsername(),
                        profile.getEmail(), profile.getMobile() == null ? "" : profile.getMobile(), profile.getRole(),
                        profile.isActive(), profile.getLastLoginAt()))
                .toList();
    }

    public Optional<RoleProfileStore.StoredProfile> findForAuthentication(String identifier) {
        return store.findForAuthentication(table, identifier);
    }

    public void updateLastLogin(Long identityId) {
        store.updateLastLogin(table, identityId);
    }

    public void delete(Long identityId) {
        store.delete(table, identityId);
    }

    public void upsert(ProfileSyncEvent event) {
        store.upsert(table, event);
    }
}
