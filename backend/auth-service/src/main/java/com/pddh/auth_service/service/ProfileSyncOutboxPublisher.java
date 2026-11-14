package com.pddh.auth_service.service;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import com.pddh.auth_service.repository.ProfileSyncOutboxRepository;

@Component
@ConditionalOnProperty(name = "app.profile-sync.publisher-enabled", havingValue = "true", matchIfMissing = true)
public class ProfileSyncOutboxPublisher {
    private final ProfileSyncOutboxRepository outbox;
    private final KafkaTemplate<String, String> kafka;

    public ProfileSyncOutboxPublisher(ProfileSyncOutboxRepository outbox, KafkaTemplate<String, String> kafka) {
        this.outbox = outbox;
        this.kafka = kafka;
    }

    @Scheduled(initialDelayString = "${app.profile-sync.publish-delay-ms:30000}",
            fixedDelayString = "${app.profile-sync.publish-delay-ms:30000}")
    @Transactional
    public void publishPending() {
        for (var record : outbox.findTop100ByPublishedAtIsNullAndNextAttemptAtBeforeOrderByCreatedAtAsc(
                java.time.Instant.now())) {
            try {
                kafka.send(record.getRoutingKey(), record.getEventId(), record.getPayload()).get(10, TimeUnit.SECONDS);
                record.setPublishedAt(java.time.Instant.now());
            } catch (RuntimeException | ExecutionException | TimeoutException exception) {
                record.setAttempts(record.getAttempts() + 1);
                long delaySeconds = Math.min(60, 1L << Math.min(record.getAttempts(), 6));
                record.setNextAttemptAt(java.time.Instant.now().plusSeconds(delaySeconds));
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new KafkaException("Interrupted while publishing profile sync event", exception);
            }
        }
    }
}
