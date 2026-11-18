package com.pddh.auth_service.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.pddh.auth_service.repository.UserRepository;
import com.pddh.auth_service.repository.ProfileSyncBackfillMarkerRepository;
import com.pddh.auth_service.entities.ProfileSyncBackfillMarker;

@Component
public class ProfileSyncBackfill {
    private static final short ROLE_TABLES_JPA_BACKFILL_ID = 2;
    private static final Logger logger = LoggerFactory.getLogger(ProfileSyncBackfill.class);
    private final JdbcTemplate jdbc;
    private final UserRepository users;
    private final ProfileSyncBackfillMarkerRepository markers;
    private final ProfileSyncService profileSync;

    public ProfileSyncBackfill(
            JdbcTemplate jdbc, UserRepository users, ProfileSyncBackfillMarkerRepository markers,
            ProfileSyncService profileSync) {
        this.jdbc = jdbc;
        this.users = users;
        this.markers = markers;
        this.profileSync = profileSync;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Async("profileSyncBackfillExecutor")
    @Transactional
    public void backfillExistingProfiles() {
        jdbc.execute((ConnectionCallback<Void>) connection -> {
            try (var statement = connection.prepareStatement("SELECT pg_advisory_xact_lock(?)")) {
                statement.setLong(1, 764321906L);
                statement.execute();
            }
            return null;
        });
        if (markers.existsById(ROLE_TABLES_JPA_BACKFILL_ID)) return;

        int page = 0;
        int copied = 0;
        while (true) {
            var batch = users.findAll(PageRequest.of(page, 250, Sort.by(Sort.Direction.ASC, "id")));
            if (batch.isEmpty()) break;
            batch.forEach(profileSync::upsert);
            copied += batch.getNumberOfElements();
            if (!batch.hasNext()) break;
            page++;
        }
        ProfileSyncBackfillMarker marker = new ProfileSyncBackfillMarker();
        marker.setId(ROLE_TABLES_JPA_BACKFILL_ID);
        marker.setCompletedAt(java.time.Instant.now());
        markers.saveAndFlush(marker);
        logger.info("Synchronized {} existing accounts into role profile tables", copied);
    }
}
