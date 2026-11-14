package com.pddh.travel_service.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pddh.travel_service.entity.OutboxEvent;
import com.pddh.travel_service.repository.OutboxEventRepository;

@Service
public class OutboxService {
    private final OutboxEventRepository repository;
    private final ObjectMapper objectMapper;

    public OutboxService(OutboxEventRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void enqueue(String aggregateId, String routingKey, Object event) {
        OutboxEvent record = new OutboxEvent();
        record.setId(UUID.randomUUID().toString());
        record.setAggregateId(aggregateId);
        record.setRoutingKey(routingKey);
        record.setPayload(serialize(event));
        record.setCreatedAt(Instant.now());
        record.setNextAttemptAt(record.getCreatedAt());
        record.setAttempts(0);
        repository.save(record);
    }

    private String serialize(Object event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Saga event could not be serialized", exception);
        }
    }
}
