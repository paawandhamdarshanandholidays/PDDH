package com.pddh.auth_service.entities;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "profile_sync_outbox")
@Getter
@Setter
@NoArgsConstructor
public class ProfileSyncOutbox {
    @Id
    @Column(length = 36, nullable = false)
    private String id;
    @Column(length = 36, nullable = false)
    private String eventId;
    @Column(length = 100, nullable = false)
    private String routingKey;
    @Column(nullable = false, columnDefinition = "text")
    private String payload;
    @Column(nullable = false)
    private Instant createdAt;
    @Column(nullable = false)
    private Instant nextAttemptAt;
    private Instant publishedAt;
    @Column(nullable = false)
    private int attempts;
}
