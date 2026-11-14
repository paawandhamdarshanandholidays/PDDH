package com.pddh.places_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;


@SpringBootApplication
@EnableJpaRepositories("com.pddh.places_service")
public class PlacesServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(PlacesServiceApplication.class, args);
    }
}
