package com.pddh.travel_service.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@EnableKafka
public class BookingSagaKafkaConfiguration {
    @Bean NewTopic bookingCreatedTopic() { return topic("pddh.booking.created"); }
    @Bean NewTopic bookingValidationResultsTopic() { return topic("pddh.booking.validation-results"); }
    @Bean NewTopic bookingCompletedTopic() { return topic("pddh.booking.completed"); }

    private NewTopic topic(String name) {
        return TopicBuilder.name(name).partitions(3).replicas(1).build();
    }
}
