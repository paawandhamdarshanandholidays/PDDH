package com.pddh.auth_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pddh.auth_service.entities.ProfileSyncBackfillMarker;

public interface ProfileSyncBackfillMarkerRepository
        extends JpaRepository<ProfileSyncBackfillMarker, Short> {
}
