package com.pddh.places_service.idempotency;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, String> {
    Optional<IdempotencyRecord> findByKeyScopeAndOperationAndIdempotencyKey(
            String keyScope, String operation, String idempotencyKey);
}
