package com.pddh.auth_service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import com.pddh.auth_service.config.PasswordConfig;
import com.pddh.auth_service.config.SecurityConfig;

class AuthServiceApplicationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(PasswordConfig.class, SecurityConfig.class)
            .withPropertyValues(
                    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration",
                    "eureka.client.enabled=false"
            );

    @Test
    void contextLoads() {
        contextRunner.run(context -> {
            assertThat(context).hasBean("passwordEncoder");
            assertThat(context).hasBean("securityFilterChain");
        });
    }

    @Test
    void constructor_shouldInstantiateApplicationClass() {
        AuthServiceApplication app = new AuthServiceApplication();
        assertThat(app).isNotNull();
    }

    @Test
    void main_shouldStartSpringApplication() {
        try (MockedStatic<SpringApplication> springApplication = mockStatic(SpringApplication.class)) {
            springApplication.when(() -> SpringApplication.run(AuthServiceApplication.class, new String[0]))
                    .thenReturn(null);

            AuthServiceApplication.main(new String[0]);

            springApplication.verify(() -> SpringApplication.run(AuthServiceApplication.class, new String[0]));
        }
    }
}

