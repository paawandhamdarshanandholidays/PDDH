package com.pddh.travel_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

import com.pddh.api_common.resilience.ResilienceConfiguration;
import com.pddh.travel_service.entity.OutboxEvent;
import com.pddh.travel_service.repository.OutboxEventRepository;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class OutboxPublisherTest {
    private OutboxEventRepository repository;
    private KafkaTemplate<String, String> kafka;
    private OutboxEvent event;
    private SimpleMeterRegistry meterRegistry;
    private OutboxPublisher publisher;

    @BeforeEach
    void setUp() {
        repository = mock(OutboxEventRepository.class);
        kafka = mock(KafkaTemplate.class);
        meterRegistry = new SimpleMeterRegistry();
        event = new OutboxEvent();
        event.setId("event-1");
        event.setAggregateId("booking-1");
        event.setRoutingKey("booking.created");
        event.setPayload("{}");
        event.setCreatedAt(Instant.now());
        event.setNextAttemptAt(Instant.now().minusSeconds(1));
        when(repository.findTop100ByPublishedAtIsNullAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(any()))
                .thenReturn(List.of(event));
        publisher = new OutboxPublisher(repository, kafka, "pddh.booking.created", "pddh.booking.completed",
                new ResilienceConfiguration().retryRegistry(), meterRegistry);
    }

    @Test
    void retriesBrokerFailuresAndMarksEventPublishedAfterSuccessfulRetry() {
        when(kafka.send("pddh.booking.created", "booking-1", "{}"))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("temporary failure")))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("temporary failure")))
                .thenReturn(CompletableFuture.completedFuture(null));

        publisher.publishPendingEvents();

        verify(kafka, times(3)).send("pddh.booking.created", "booking-1", "{}");
        assertThat(event.getPublishedAt()).isNotNull();
        assertThat(event.getAttempts()).isZero();
        assertThat(meterRegistry.counter("pddh.saga.publish.retries").count()).isEqualTo(2);
        verify(repository).saveAll(List.of(event));
    }

    @Test
    void recordsFailedPublishAndSchedulesOutboxRetryAfterRetryExhaustion() {
        when(kafka.send("pddh.booking.created", "booking-1", "{}"))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("broker unavailable")));

        publisher.publishPendingEvents();

        verify(kafka, times(3)).send("pddh.booking.created", "booking-1", "{}");
        assertThat(event.getPublishedAt()).isNull();
        assertThat(event.getAttempts()).isEqualTo(1);
        assertThat(event.getNextAttemptAt()).isAfter(Instant.now());
        assertThat(meterRegistry.counter("pddh.saga.events.failed").count()).isEqualTo(1);
        verify(repository).saveAll(List.of(event));
    }
}
