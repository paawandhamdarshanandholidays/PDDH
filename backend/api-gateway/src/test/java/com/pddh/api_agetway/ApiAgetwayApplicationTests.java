package com.pddh.api_agetway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.server.mvc.config.GatewayMvcProperties;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = com.pddh.api_gateway.ApiGatewayApplication.class)
@TestPropertySource(properties = {
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "app.jwt.secret=12345678901234567890123456789012",
        "app.gateway.internal-token=test-gateway-internal-token-at-least-32-bytes"
})
class ApiAgetwayApplicationTests {

    @Autowired
    private GatewayMvcProperties gatewayMvcProperties;

	@Test
	void contextLoads() {
	}

    @Test
    void everyRouteSetsTheInternalGatewayToken() {
        assertThat(gatewayMvcProperties.getRoutes())
                .isNotEmpty()
                .allSatisfy(route -> assertThat(route.getFilters())
                        .anySatisfy(filter -> {
                            assertThat(filter.getName()).isEqualTo("SetRequestHeader");
                            assertThat(filter.getArgs().values())
                                    .contains("X-Internal-Gateway-Token");
                        }));
    }

}
