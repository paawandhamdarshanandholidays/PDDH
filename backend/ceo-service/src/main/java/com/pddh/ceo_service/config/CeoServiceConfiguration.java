package com.pddh.ceo_service.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RoleDashboardProperties.class)
public class CeoServiceConfiguration {
}