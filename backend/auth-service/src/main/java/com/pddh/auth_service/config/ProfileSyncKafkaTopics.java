package com.pddh.auth_service.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class ProfileSyncKafkaTopics {
    @Bean NewTopic customerProfileTopic() { return topic("pddh.profile.customer"); }
    @Bean NewTopic adminProfileTopic() { return topic("pddh.profile.admin"); }
    @Bean NewTopic managerProfileTopic() { return topic("pddh.profile.manager"); }
    @Bean NewTopic ceoProfileTopic() { return topic("pddh.profile.ceo"); }
    @Bean NewTopic staffProfileTopic() { return topic("pddh.profile.staff"); }

    private NewTopic topic(String name) {
        return TopicBuilder.name(name).partitions(3).replicas(1).build();
    }
}
