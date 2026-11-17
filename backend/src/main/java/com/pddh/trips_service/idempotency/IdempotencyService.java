package com.pddh.trips_service.idempotency;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Supplier;

import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

@Service
public class IdempotencyService {
    private final IdempotencyRecordRepository repository;
    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbcTemplate;
    private final String scopeSecret;

    public IdempotencyService(
            IdempotencyRecordRepository repository,
            ObjectMapper objectMapper,
            JdbcTemplate jdbcTemplate,
            @Value("${app.idempotency.scope-secret:${app.jwt.secret}}") String scopeSecret) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.jdbcTemplate = jdbcTemplate;
        this.scopeSecret = scopeSecret;
    }

    @Transactional
    public <T> T execute(
            String keyScope, String operation, String key, Object request, Class<T> responseType, Supplier<T> action) {
        validateKey(key);
        String hashedScope = scopeFingerprint(keyScope);
        String lockIdentity = hashedScope + '\n' + operation + '\n' + key;
        jdbcTemplate.execute((ConnectionCallback<Void>) connection -> acquireLock(connection, lockIdentity));

        String requestHash = fingerprint(request);
        var existing = repository.findByKeyScopeAndOperationAndIdempotencyKey(hashedScope, operation, key);
        if (existing.isPresent()) {
            IdempotencyRecord record = existing.get();
            if (!MessageDigest.isEqual(
                    record.getRequestHash().getBytes(StandardCharsets.US_ASCII),
                    requestHash.getBytes(StandardCharsets.US_ASCII))) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "This Idempotency-Key was already used with a different request");
            }
            return deserialize(record.getResponseBody(), responseType);
        }

        T response = action.get();
        IdempotencyRecord record = new IdempotencyRecord();
        record.setId(UUID.randomUUID().toString());
        record.setKeyScope(hashedScope);
        record.setOperation(operation);
        record.setIdempotencyKey(key);
        record.setRequestHash(requestHash);
        record.setResponseBody(serialize(response));
        record.setCreatedAt(Instant.now());
        repository.saveAndFlush(record);
        return response;
    }

    private Void acquireLock(Connection connection, String lockIdentity) throws java.sql.SQLException {
        try (var statement = connection.prepareStatement(
                "SELECT pg_advisory_xact_lock(hashtextextended(?, 0))")) {
            statement.setString(1, lockIdentity);
            statement.execute();
        }
        return null;
    }

    private void validateKey(String key) {
        if (key == null || !key.matches("[!-~]{8,128}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Idempotency-Key must contain 8 to 128 visible ASCII characters");
        }
    }

    private String fingerprint(Object request) {
        try {
            byte[] payload = objectMapper.writeValueAsBytes(canonicalize(objectMapper.valueToTree(request)));
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(payload));
        } catch (JsonProcessingException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request cannot be fingerprinted", exception);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private String scopeFingerprint(String keyScope) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(scopeSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return java.util.HexFormat.of().formatHex(mac.doFinal(keyScope.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("HMAC-SHA256 is unavailable", exception);
        }
    }

    private JsonNode canonicalize(JsonNode node) {
        if (node.isObject()) {
            ObjectNode sorted = JsonNodeFactory.instance.objectNode();
            Map<String, JsonNode> fields = new TreeMap<>();
            node.fields().forEachRemaining(entry -> fields.put(entry.getKey(), entry.getValue()));
            fields.forEach((name, value) -> sorted.set(name, canonicalize(value)));
            return sorted;
        }
        if (node.isArray()) {
            ArrayNode sorted = JsonNodeFactory.instance.arrayNode();
            node.forEach(value -> sorted.add(canonicalize(value)));
            return sorted;
        }
        return node.deepCopy();
    }

    private String serialize(Object response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException exception) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Response could not be stored for idempotent replay", exception);
        }
    }

    private <T> T deserialize(String responseBody, Class<T> responseType) {
        try {
            return objectMapper.readValue(responseBody, responseType);
        } catch (JsonProcessingException exception) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Stored idempotent response is invalid", exception);
        }
    }
}
