package com.pddh.auth_service.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

class SecurityConfigTest {
    @Test
    void securityConfig_shouldCreateFilterChainBean() throws Exception {
        SecurityConfig config = new SecurityConfig();
        HttpSecurity http = mock(HttpSecurity.class);
        DefaultSecurityFilterChain expected = mock(DefaultSecurityFilterChain.class);

        when(http.csrf(any())).thenReturn(http);
        when(http.sessionManagement(any())).thenReturn(http);
        when(http.authorizeHttpRequests(any())).thenReturn(http);
        when(http.build()).thenReturn(expected);

        SecurityFilterChain securityFilterChain = config.securityFilterChain(http);

        assertThat(securityFilterChain).isSameAs(expected);
    }

    @Test
    void isGatewayRequest_shouldValidateHeader() {
        SecurityConfig config = new SecurityConfig();
        MockHttpServletRequest allowedRequest = new MockHttpServletRequest();
        allowedRequest.addHeader("X-Gateway-Request", "true");
        MockHttpServletRequest deniedRequest = new MockHttpServletRequest();

        assertThat(config.isGatewayRequest((HttpServletRequest) allowedRequest)).isTrue();
        assertThat(config.isGatewayRequest((HttpServletRequest) deniedRequest)).isFalse();
        assertThat(config.authorizeGatewayRequest(allowedRequest).isGranted()).isTrue();
        assertThat(config.authorizeGatewayRequest(deniedRequest).isGranted()).isFalse();
        assertThat(config.authorizeGatewayRequest(null, new RequestAuthorizationContext(allowedRequest)).isGranted())
                .isTrue();
        assertThat(config.authorizeGatewayRequest(null, new RequestAuthorizationContext(deniedRequest)).isGranted())
                .isFalse();
        MockHttpServletRequest docsRequest = new MockHttpServletRequest("GET", "/v3/api-docs");
        docsRequest.addHeader("X-Gateway-Request", "true");
        assertThat(config.authorizeGatewayRequest(docsRequest).isGranted()).isTrue();
    }
}
