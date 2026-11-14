package com.pddh.stays_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;


@SpringBootApplication
@EnableJpaRepositories("com.pddh.stays_service")
public class StaysServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(StaysServiceApplication.class, args);
    }
}
