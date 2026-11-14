package com.pddh.api_gateway;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfigurationSource;

import com.pddh.api_gateway.config.SecurityConfig;
import com.pddh.api_gateway.security.JwtAuthenticationFilter;

class SecurityConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withPropertyValues("app.jwt.secret=12345678901234567890123456789012")
            .withUserConfiguration(SecurityConfig.class, JwtAuthenticationFilter.class);

    @Test
    void contextLoadsAndSecurityBeansExist() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(SecurityFilterChain.class);
            assertThat(context.getBean(SecurityFilterChain.class).getFilters())
                    .noneMatch(BasicAuthenticationFilter.class::isInstance);
            AuthorizationFilter authorizationFilter = context.getBean(SecurityFilterChain.class).getFilters().stream()
                    .filter(AuthorizationFilter.class::isInstance)
                    .map(AuthorizationFilter.class::cast)
                    .findFirst()
                    .orElseThrow();
            MockHttpServletRequest availabilityRequest =
                    new MockHttpServletRequest("GET", "/auth/username-availability");
            assertThat(authorizationFilter.getAuthorizationManager()
                    .authorize(() -> null, availabilityRequest).isGranted()).isTrue();
            MockHttpServletRequest openApiRequest = new MockHttpServletRequest("GET", "/api-docs/services/auth-service");
            assertThat(authorizationFilter.getAuthorizationManager()
                    .authorize(() -> null, openApiRequest).isGranted()).isTrue();
            MockHttpServletRequest privateRequest = new MockHttpServletRequest("GET", "/api/users/me");
            assertThat(authorizationFilter.getAuthorizationManager()
                    .authorize(() -> null, privateRequest).isGranted()).isFalse();
            MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/bookings");
            assertThat(context.getBean(CorsConfigurationSource.class)
                    .getCorsConfiguration(request)
                    .getAllowedHeaders()).contains("Idempotency-Key");
        });
    }
}
