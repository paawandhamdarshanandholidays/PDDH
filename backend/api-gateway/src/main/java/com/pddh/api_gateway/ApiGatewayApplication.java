package com.pddh.api_gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Import;

import com.pddh.api_common.exception.GlobalExceptionHandler;
import com.pddh.api_common.resilience.RateLimitGuard;
import com.pddh.api_common.resilience.ResilienceConfiguration;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@EnableDiscoveryClient
@Import({GlobalExceptionHandler.class, RateLimitGuard.class, ResilienceConfiguration.class})
public class ApiGatewayApplication {
	static Logger logger=LoggerFactory.getLogger(ApiGatewayApplication.class);

	public static void main(String[] args) {
		logger.info("ApiGatewayApplication main method called");
		SpringApplication.run(ApiGatewayApplication.class, args);
	}

}
