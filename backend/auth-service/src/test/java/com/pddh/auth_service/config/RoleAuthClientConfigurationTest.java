package com.pddh.auth_service.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.web.reactive.function.client.WebClient;

class RoleAuthClientConfigurationTest {
    @Test
    void registersLoadBalancedWebClientBuilder() {
        new ApplicationContextRunner()
                .withUserConfiguration(RoleAuthClientConfiguration.class)
                .run(context -> assertThat(context).hasSingleBean(WebClient.Builder.class));
    }
}
