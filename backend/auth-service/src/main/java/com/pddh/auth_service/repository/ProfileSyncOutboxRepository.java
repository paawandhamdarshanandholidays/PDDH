package com.pddh.auth_service.repository;

import java.util.List;
import java.time.Instant;

import org.springframework.data.jpa.repository.JpaRepository;
import com.pddh.auth_service.entities.ProfileSyncOutbox;

public interface ProfileSyncOutboxRepository extends JpaRepository<ProfileSyncOutbox, String> {
    List<ProfileSyncOutbox> findTop100ByPublishedAtIsNullAndNextAttemptAtBeforeOrderByCreatedAtAsc(Instant now);
}
