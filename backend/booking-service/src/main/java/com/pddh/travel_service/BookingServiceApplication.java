package com.pddh.travel_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.pddh.api_common.exception.GlobalExceptionHandler;
import com.pddh.api_common.resilience.RateLimitGuard;
import com.pddh.api_common.resilience.ResilienceConfiguration;
import com.pddh.api_common.security.GatewayAccessFilter;

@SpringBootApplication
@Import({GlobalExceptionHandler.class, RateLimitGuard.class, ResilienceConfiguration.class, GatewayAccessFilter.class})
@EnableScheduling
public class BookingServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(BookingServiceApplication.class, args);
    }
}
