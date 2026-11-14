package com.pddh.stays_service.idempotency;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "idempotency_records", uniqueConstraints = @UniqueConstraint(
        name = "uk_idempotency_scope_operation_key",
        columnNames = { "key_scope", "operation", "idempotency_key" }))
public class IdempotencyRecord {
    @Id
    @Column(nullable = false, length = 36)
    private String id;

    @Column(name = "key_scope", nullable = false, length = 255)
    private String keyScope;

    @Column(nullable = false, length = 255)
    private String operation;

    @Column(name = "idempotency_key", nullable = false, length = 128)
    private String idempotencyKey;

    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;

    @Column(name = "response_body", nullable = false, columnDefinition = "TEXT")
    private String responseBody;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
