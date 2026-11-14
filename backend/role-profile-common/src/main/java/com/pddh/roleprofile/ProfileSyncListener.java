package com.pddh.roleprofile;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;

public class ProfileSyncListener {
    private final RoleProfileService profiles;
    private final ObjectMapper mapper;

    public ProfileSyncListener(RoleProfileService profiles, ObjectMapper mapper) {
        this.profiles = profiles;
        this.mapper = mapper;
    }

    @KafkaListener(topics = "${app.profile-sync.topic}", groupId = "${spring.application.name}")
    public void receive(String payload) throws Exception {
        profiles.sync(mapper.readValue(payload, ProfileSyncEvent.class));
    }
}
