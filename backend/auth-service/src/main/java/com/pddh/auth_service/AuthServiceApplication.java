package com.pddh.auth_service;

import org.slf4j.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Import;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.scheduling.annotation.EnableScheduling;
import com.pddh.api_common.exception.GlobalExceptionHandler;
import com.pddh.api_common.resilience.RateLimitGuard;
import com.pddh.api_common.resilience.ResilienceConfiguration;
import com.pddh.api_common.security.GatewayAccessFilter;
import com.pddh.roleprofile.RoleProfileStore;

@SpringBootApplication
@EnableDiscoveryClient
@EnableScheduling
@Import({GlobalExceptionHandler.class, RateLimitGuard.class, ResilienceConfiguration.class,
		GatewayAccessFilter.class, RoleProfileStore.class})
@EntityScan(basePackages = {"com.pddh.auth_service.entities", "com.pddh.auth_service.idempotency", "com.pddh.roleprofile.entities"})
public class AuthServiceApplication {
 static Logger logger=LoggerFactory.getLogger(AuthServiceApplication.class);
	public static void main(String[] args) {
		logger.info("AuthServiceApplication main method called");
		SpringApplication.run(AuthServiceApplication.class, args);
	}

}
