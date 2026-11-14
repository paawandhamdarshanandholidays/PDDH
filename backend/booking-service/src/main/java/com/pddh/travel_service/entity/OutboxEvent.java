package com.pddh.travel_service.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "outbox_events")
public class OutboxEvent {
    @Id
    @Column(nullable = false, length = 36)
    private String id;
    @Column(nullable = false, length = 36)
    private String aggregateId;
    @Column(nullable = false, length = 100)
    private String routingKey;
    @Column(nullable = false, columnDefinition = "text")
    private String payload;
    @Column(nullable = false)
    private Instant createdAt;
    @Column(nullable = false)
    private Instant nextAttemptAt;
    @Column
    private Instant publishedAt;
    @Column(nullable = false)
    private int attempts;
}
