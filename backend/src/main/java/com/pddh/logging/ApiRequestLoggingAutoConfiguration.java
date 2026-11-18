package com.pddh.logging;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class ApiRequestLoggingAutoConfiguration {

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    ApiRequestLoggingFilter apiRequestLoggingFilter() {
        return new ApiRequestLoggingFilter();
    }
}
