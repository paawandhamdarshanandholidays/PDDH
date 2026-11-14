package com.pddh.gallery_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;


@SpringBootApplication
@EnableJpaRepositories("com.pddh.gallery_service")
public class GalleryServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(GalleryServiceApplication.class, args);
    }
}
