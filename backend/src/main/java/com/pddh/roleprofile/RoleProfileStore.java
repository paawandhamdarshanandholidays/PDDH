package com.pddh.roleprofile;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Supplier;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.pddh.roleprofile.entities.AdminRoleProfile;
import com.pddh.roleprofile.entities.CeoRoleProfile;
import com.pddh.roleprofile.entities.ManagerRoleProfile;
import com.pddh.roleprofile.entities.RoleProfileFields;
import com.pddh.roleprofile.entities.StaffRoleProfile;
import com.pddh.roleprofile.entities.UserRoleProfile;

import jakarta.persistence.EntityManager;

@Repository
public class RoleProfileStore {
    private final EntityManager entityManager;

    public RoleProfileStore(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Transactional(readOnly = true)
    public long count(String table) {
        EntityType type = entityType(table);
        return entityManager.createQuery("select count(profile) from " + type.name + " profile", Long.class)
                .getSingleResult();
    }

    @Transactional(readOnly = true)
    public List<RoleProfileFields> page(String table, int size, long offset) {
        if (offset > Integer.MAX_VALUE) {
            return List.of();
        }
        EntityType type = entityType(table);
        return entityManager.createQuery("select profile from " + type.name + " profile order by profile.id desc",
                type.entityClass)
                .setMaxResults(size)
                .setFirstResult(Math.toIntExact(offset))
                .getResultList()
                .stream()
                .map(profile -> (RoleProfileFields) profile)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<StoredProfile> findForAuthentication(String table, String identifier) {
        EntityType type = entityType(table);
        return entityManager.createQuery("select profile from " + type.name
                        + " profile where lower(profile.username) = lower(:identifier)"
                        + " or lower(profile.email) = lower(:identifier)"
                        + " or lower(profile.mobile) = lower(:identifier)", type.entityClass)
                .setParameter("identifier", identifier)
                .setMaxResults(1)
                .getResultStream()
                .map(profile -> (RoleProfileFields) profile)
                .map(profile -> new StoredProfile(profile.getIdentityId(), profile.getUsername(),
                        profile.getEmail(), profile.getPasswordHash(), profile.isActive()))
                .findFirst();
    }

    @Transactional
    public void updateLastLogin(String table, Long identityId) {
        findByIdentityId(table, identityId).ifPresent(profile -> {
            profile.setLastLoginAt(Instant.now());
            profile.setUpdatedAt(Instant.now());
        });
    }

    @Transactional
    public void delete(String table, Long identityId) {
        findByIdentityId(table, identityId).ifPresent(entityManager::remove);
    }

    @Transactional
    public void upsert(String table, ProfileSyncEvent event) {
        EntityType type = entityType(table);
        RoleProfileFields profile = findByIdentityId(type, event.identityId())
                .orElseGet(type.factory);
        profile.setIdentityId(event.identityId());
        profile.setRole(event.role().trim().toUpperCase(Locale.ROOT));
        profile.setName(event.name());
        profile.setUsername(event.username());
        profile.setEmail(event.email());
        profile.setPasswordHash(event.passwordHash());
        profile.setMobile(event.mobile());
        profile.setGender(event.gender());
        profile.setActive(event.active());
        profile.setUpdatedAt(Instant.now());
        if (entityManager.contains(profile)) {
            return;
        }
        if (profile.getCreatedAt() == null) {
            profile.setCreatedAt(Instant.now());
        }
        if (profile instanceof UserRoleProfile entity) entityManager.persist(entity);
        else if (profile instanceof AdminRoleProfile entity) entityManager.persist(entity);
        else if (profile instanceof ManagerRoleProfile entity) entityManager.persist(entity);
        else if (profile instanceof CeoRoleProfile entity) entityManager.persist(entity);
        else if (profile instanceof StaffRoleProfile entity) entityManager.persist(entity);
        else throw new IllegalStateException("Unsupported role profile entity");
    }

    private Optional<RoleProfileFields> findByIdentityId(String table, Long identityId) {
        return findByIdentityId(entityType(table), identityId);
    }

    private Optional<RoleProfileFields> findByIdentityId(EntityType type, Long identityId) {
        return entityManager.createQuery("select profile from " + type.name
                        + " profile where profile.identityId = :identityId", type.entityClass)
                .setParameter("identityId", identityId)
                .getResultStream()
                .map(profile -> (RoleProfileFields) profile)
                .findFirst();
    }

    private EntityType entityType(String table) {
        return switch (table) {
            case "users" -> new EntityType("UserRoleProfile", UserRoleProfile.class, UserRoleProfile::new);
            case "admin" -> new EntityType("AdminRoleProfile", AdminRoleProfile.class, AdminRoleProfile::new);
            case "manager" -> new EntityType("ManagerRoleProfile", ManagerRoleProfile.class, ManagerRoleProfile::new);
            case "ceo" -> new EntityType("CeoRoleProfile", CeoRoleProfile.class, CeoRoleProfile::new);
            case "staff" -> new EntityType("StaffRoleProfile", StaffRoleProfile.class, StaffRoleProfile::new);
            default -> throw new IllegalArgumentException("Unsupported role profile table: " + table);
        };
    }

    private record EntityType(String name, Class<? extends RoleProfileFields> entityClass,
            Supplier<? extends RoleProfileFields> factory) { }

    public record StoredProfile(Long identityId, String username, String email, String passwordHash, boolean active) { }
}
