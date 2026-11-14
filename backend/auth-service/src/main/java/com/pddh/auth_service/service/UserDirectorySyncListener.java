package com.pddh.auth_service.service;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pddh.roleprofile.ProfileSyncEvent;

@Component
public class UserDirectorySyncListener {
    private final UserDirectoryService directoryService;
    private final ObjectMapper objectMapper;

    public UserDirectorySyncListener(UserDirectoryService directoryService, ObjectMapper objectMapper) {
        this.directoryService = directoryService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = "${app.user-directory.profile-sync-topic:pddh.profile.customer}",
            groupId = "${spring.application.name}-user-directory")
    public void receive(String payload) {
        try {
            directoryService.syncCustomer(objectMapper.readValue(payload, ProfileSyncEvent.class));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not read customer profile sync event", exception);
        }
    }
}
