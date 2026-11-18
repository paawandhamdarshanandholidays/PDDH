package com.pddh.auth_service.entities;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "profile_sync_backfill")
public class ProfileSyncBackfillMarker {
    @Id
    private Short id;
    @Column(name = "completed_at", nullable = false)
    private Instant completedAt;

    public Short getId() { return id; }
    public void setId(Short id) { this.id = id; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
}
