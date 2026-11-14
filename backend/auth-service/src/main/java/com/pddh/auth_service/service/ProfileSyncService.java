package com.pddh.auth_service.service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pddh.auth_service.dto.ProfileSyncEvent;
import com.pddh.auth_service.entities.ProfileSyncOutbox;
import com.pddh.auth_service.entities.User;
import com.pddh.auth_service.repository.ProfileSyncOutboxRepository;
import com.pddh.roleprofile.RoleProfileStore;

@Service
public class ProfileSyncService {
    private static final Map<String, String> ROLE_TABLES = Map.of(
            "CUSTOMER", "users",
            "ADMIN", "admin",
            "MANAGER", "manager",
            "CEO", "ceo",
            "STAFF", "staff");

    private final ProfileSyncOutboxRepository outbox;
    private final ObjectMapper mapper;
    private final RoleProfileStore profiles;

    public ProfileSyncService(
            ProfileSyncOutboxRepository outbox, ObjectMapper mapper, RoleProfileStore profiles) {
        this.outbox = outbox;
        this.mapper = mapper;
        this.profiles = profiles;
    }

    @Transactional
    public void upsert(User user) {
        profiles.upsert(tableFor(user.getRole()), toProfileEvent(user, "UPSERT"));
        enqueue(user, "UPSERT");
    }

    @Transactional
    public void delete(User user) {
        profiles.delete(tableFor(user.getRole()), user.getId());
        enqueue(user, "DELETE");
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

    private void enqueue(User user, String operation) {
        String eventId = UUID.randomUUID().toString();
        ProfileSyncEvent event = new ProfileSyncEvent(eventId, operation, user.getId(),
                user.getRole().toUpperCase(), user.getName(), user.getUsername(), user.getEmail(), user.getPassword(),
                user.getMobile(), user.getGender(), user.isActive());
        ProfileSyncOutbox record = new ProfileSyncOutbox();
        record.setId(UUID.randomUUID().toString());
        record.setEventId(eventId);
        record.setRoutingKey("pddh.profile." + user.getRole().toLowerCase());
        try {
            record.setPayload(mapper.writeValueAsString(event));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize profile sync event", exception);
        }
        record.setCreatedAt(Instant.now());
        record.setNextAttemptAt(Instant.now());
        record.setAttempts(0);
        outbox.save(record);
    }
}
