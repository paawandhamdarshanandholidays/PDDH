package com.pddh.api_common.resilience;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.kafka.KafkaException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import io.github.resilience4j.retry.Retry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class ResilienceConfigurationTest {
    private final ResilienceConfiguration configuration = new ResilienceConfiguration();

    @Test
    void rateLimiterRejectsRequestsAfterSixtyWithinTheRefreshPeriod() {
        RateLimitGuard guard = new RateLimitGuard(configuration.rateLimiterRegistry(), new SimpleMeterRegistry());

        for (int request = 0; request < 60; request++) {
            guard.check("test.operation");
        }

        assertThatThrownBy(() -> guard.check("test.operation"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("status")
                .isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    void retryRepeatsKafkaFailuresAndStopsAfterSuccess() {
        Retry retry = configuration.retryRegistry().retry("test.kafka");
        AtomicInteger attempts = new AtomicInteger();

        assertThatCode(() -> Retry.decorateRunnable(retry, () -> {
            if (attempts.incrementAndGet() < 3) {
                throw new KafkaException("temporary broker failure");
            }
        }).run()).doesNotThrowAnyException();

        assertThat(attempts).hasValue(3);
    }

    @Test
    void retryDoesNotRepeatNonKafkaFailures() {
        Retry retry = configuration.retryRegistry().retry("test.non-kafka");
        AtomicInteger attempts = new AtomicInteger();

        assertThatThrownBy(() -> Retry.decorateRunnable(retry, () -> {
            attempts.incrementAndGet();
            throw new IllegalStateException("permanent failure");
        }).run()).isInstanceOf(IllegalStateException.class);

        assertThat(attempts).hasValue(1);
    }
}
