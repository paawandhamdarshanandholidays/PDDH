package com.pddh.auth_service.idempotency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.ObjectMapper;

class IdempotencyServiceTest {
    private final IdempotencyRecordRepository repository = mock(IdempotencyRecordRepository.class);
    private final IdempotencyService service =
            new IdempotencyService(repository, new ObjectMapper(), mock(JdbcTemplate.class), "test-scope-secret");

    @Test
    void execute_replaysSuccessfulResponseWithoutRepeatingAction() {
        var request = new Request("details");
        var stored = new IdempotencyRecord();
        AtomicInteger invocations = new AtomicInteger();
        when(repository.findByKeyScopeAndOperationAndIdempotencyKey(anyString(), anyString(), anyString()))
                .thenReturn(Optional.empty(), Optional.of(stored));
        when(repository.saveAndFlush(org.mockito.ArgumentMatchers.any(IdempotencyRecord.class)))
                .thenAnswer(invocation -> {
                    IdempotencyRecord record = invocation.getArgument(0);
                    stored.setRequestHash(record.getRequestHash());
                    stored.setResponseBody(record.getResponseBody());
                    return record;
                });

        String first = service.execute("person@example.com", "booking.create", "request-key-123",
                request, String.class, () -> {
                    invocations.incrementAndGet();
                    return "created";
                });

        String replay = service.execute("person@example.com", "booking.create", "request-key-123",
                request, String.class, () -> {
                    invocations.incrementAndGet();
                    return "created-again";
                });

        assertThat(first).isEqualTo("created");
        assertThat(replay).isEqualTo("created");
        assertThat(invocations).hasValue(1);
        verify(repository).saveAndFlush(org.mockito.ArgumentMatchers.any(IdempotencyRecord.class));
    }

    @Test
    void execute_rejectsReusingKeyForDifferentPayload() {
        var stored = new IdempotencyRecord();
        stored.setRequestHash("0".repeat(64));
        stored.setResponseBody("\"created\"");
        when(repository.findByKeyScopeAndOperationAndIdempotencyKey(anyString(), anyString(), anyString()))
                .thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> service.execute("person@example.com", "booking.create", "request-key-123",
                new Request("different"), String.class, () -> "created"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("already used with a different request");
    }

    @Test
    void execute_rejectsMissingOrInvalidKey() {
        assertThatThrownBy(() -> service.execute("scope", "operation", "bad", new Request("details"),
                String.class, () -> "created"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("8 to 128");
    }

    private record Request(String value) {
    }
}
