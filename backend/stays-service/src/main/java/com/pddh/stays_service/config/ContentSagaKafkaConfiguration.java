package com.pddh.stays_service.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@EnableKafka
@ConditionalOnProperty(name = "app.saga.enabled", havingValue = "true")
public class ContentSagaKafkaConfiguration {
    @Bean
    NewTopic bookingCreatedTopic(@Value("${app.saga.booking-created-topic}") String name) {
        return TopicBuilder.name(name).partitions(3).replicas(1).build();
    }

    @Bean
    NewTopic validationResultsTopic(@Value("${app.saga.validation-results-topic}") String name) {
        return TopicBuilder.name(name).partitions(3).replicas(1).build();
    }
}
