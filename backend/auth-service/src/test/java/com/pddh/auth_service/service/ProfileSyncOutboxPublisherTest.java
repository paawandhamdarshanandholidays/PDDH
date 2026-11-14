package com.pddh.auth_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.kafka.support.SendResult;

import com.pddh.auth_service.entities.ProfileSyncOutbox;
import com.pddh.auth_service.repository.ProfileSyncOutboxRepository;

class ProfileSyncOutboxPublisherTest {
    @Test
    void waitsThirtySecondsBeforePollingAndBetweenPollsByDefault() throws Exception {
        Scheduled schedule = ProfileSyncOutboxPublisher.class.getMethod("publishPending").getAnnotation(Scheduled.class);
        assertThat(schedule.initialDelayString()).isEqualTo("${app.profile-sync.publish-delay-ms:30000}");
        assertThat(schedule.fixedDelayString()).isEqualTo("${app.profile-sync.publish-delay-ms:30000}");
    }

    @Test
    void skipsBrokerWorkWhenNoOutboxRecordsArePending() {
        ProfileSyncOutboxRepository outbox = mock(ProfileSyncOutboxRepository.class);
        KafkaTemplate<String, String> kafka = mock(KafkaTemplate.class);
        when(outbox.findTop100ByPublishedAtIsNullAndNextAttemptAtBeforeOrderByCreatedAtAsc(any())).thenReturn(List.of());
        new ProfileSyncOutboxPublisher(outbox, kafka).publishPending();
        verify(kafka, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    void publishesPendingRecordAndMarksItPublished() {
        ProfileSyncOutboxRepository outbox = mock(ProfileSyncOutboxRepository.class);
        KafkaTemplate<String, String> kafka = mock(KafkaTemplate.class);
        ProfileSyncOutbox record = record();
        when(outbox.findTop100ByPublishedAtIsNullAndNextAttemptAtBeforeOrderByCreatedAtAsc(any())).thenReturn(List.of(record));
        when(kafka.send("pddh.profile.customer", "event-id", "{}")).thenReturn(CompletableFuture.completedFuture(null));

        new ProfileSyncOutboxPublisher(outbox, kafka).publishPending();

        verify(kafka).send("pddh.profile.customer", "event-id", "{}");
        assertThat(record.getPublishedAt()).isNotNull();
    }

    @Test
    void failedPublishSchedulesBackoffRetry() {
        ProfileSyncOutboxRepository outbox = mock(ProfileSyncOutboxRepository.class);
        KafkaTemplate<String, String> kafka = mock(KafkaTemplate.class);
        ProfileSyncOutbox record = record();
        when(outbox.findTop100ByPublishedAtIsNullAndNextAttemptAtBeforeOrderByCreatedAtAsc(any())).thenReturn(List.of(record));
        when(kafka.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("broker unavailable")));

        new ProfileSyncOutboxPublisher(outbox, kafka).publishPending();

        assertThat(record.getAttempts()).isEqualTo(1);
        assertThat(record.getNextAttemptAt()).isAfter(Instant.now());
        assertThat(record.getPublishedAt()).isNull();
    }

    private ProfileSyncOutbox record() {
        ProfileSyncOutbox record = new ProfileSyncOutbox();
        record.setId("record-id");
        record.setEventId("event-id");
        record.setRoutingKey("pddh.profile.customer");
        record.setPayload("{}");
        record.setCreatedAt(Instant.now());
        record.setNextAttemptAt(Instant.now());
        return record;
    }
}
