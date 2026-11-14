package com.pddh.roleprofile;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.TopicBuilder;
import org.apache.kafka.clients.admin.NewTopic;
import jakarta.persistence.EntityManager;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
@EnableKafka
public class RoleProfileConfiguration {
    @Bean RoleProfileProperties roleProfileProperties(@Value("${app.profile-sync.table}") String table,
            @Value("${app.role.expected}") String role, @Value("${app.role.path}") String path,
            @Value("${app.profile-sync.topic}") String topic) {
        return new RoleProfileProperties(table, role, path, topic);
    }
    @Bean RoleProfileStore roleProfileStore(EntityManager entityManager) {
        return new RoleProfileStore(entityManager);
    }
    @Bean RoleProfileSchemaCompatibility roleProfileSchemaCompatibility(
            JdbcTemplate jdbc, RoleProfileProperties properties) {
        return new RoleProfileSchemaCompatibility(jdbc, properties.table());
    }
    @Bean RoleProfileRepository roleProfileRepository(RoleProfileStore store, RoleProfileProperties properties) {
        return new RoleProfileRepository(store, properties);
    }
    @Bean RoleProfileService roleProfileService(RoleProfileRepository repository, RoleProfileProperties properties) {
        return new RoleProfileService(repository, properties);
    }
    @Bean RoleProfileController roleProfileController(RoleProfileService service, RoleIdentityService identities) {
        return new RoleProfileController(service, identities);
    }
    @Bean ProfileSyncListener profileSyncListener(RoleProfileService service, ObjectMapper mapper) {
        return new ProfileSyncListener(service, mapper);
    }
    @Bean NewTopic roleProfileTopic(RoleProfileProperties properties) {
        return TopicBuilder.name(properties.topic()).partitions(3).replicas(1).build();
    }
}
