package com.pddh.trips_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;


@SpringBootApplication
@EnableJpaRepositories("com.pddh.trips_service")
public class TripsServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(TripsServiceApplication.class, args);
    }
}
