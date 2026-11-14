package com.pddh.temples_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;


@SpringBootApplication
@EnableJpaRepositories("com.pddh.temples_service")
public class TemplesServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(TemplesServiceApplication.class, args);
    }
}
