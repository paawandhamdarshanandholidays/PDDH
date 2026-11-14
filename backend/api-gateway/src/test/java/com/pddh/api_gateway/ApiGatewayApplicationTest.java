package com.pddh.api_gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;

class ApiGatewayApplicationTest {

    @Test
    void constructor_shouldInstantiateApplicationClass() {
        ApiGatewayApplication app = new ApiGatewayApplication();

        assertThat(app).isNotNull();
    }

    @Test
    void main_shouldStartSpringApplication() {
        try (MockedStatic<SpringApplication> springApplication = mockStatic(SpringApplication.class)) {
            springApplication.when(() -> SpringApplication.run(ApiGatewayApplication.class, new String[0]))
                    .thenReturn(null);

            ApiGatewayApplication.main(new String[0]);

            springApplication.verify(() -> SpringApplication.run(ApiGatewayApplication.class, new String[0]));
        }
    }
}
