package com.pddh.api_common.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class GatewayAccessFilterTest {
    private static final String TOKEN = "test-internal-gateway-token-with-32-bytes";

    @Test
    void requestWithGatewayTokenContinuesToService() throws IOException, jakarta.servlet.ServletException {
        GatewayAccessFilter filter = new GatewayAccessFilter(TOKEN);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Internal-Gateway-Token", TOKEN);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(chain.getRequest()).isSameAs(request);
    }

    @Test
    void requestWithoutGatewayTokenIsRejected() throws IOException, jakarta.servlet.ServletException {
        assertRejected(null);
    }

    @Test
    void requestWithIncorrectGatewayTokenIsRejected() throws IOException, jakarta.servlet.ServletException {
        assertRejected("incorrect-gateway-token");
    }

    @Test
    void actuatorProbeDoesNotRequireGatewayToken() throws IOException, jakarta.servlet.ServletException {
        GatewayAccessFilter filter = new GatewayAccessFilter(TOKEN);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(chain.getRequest()).isSameAs(request);
    }

    private void assertRejected(String token) throws IOException, jakarta.servlet.ServletException {
        GatewayAccessFilter filter = new GatewayAccessFilter(TOKEN);
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (token != null) {
            request.addHeader("X-Internal-Gateway-Token", token);
        }
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(403);
    }
}
