package com.pddh.logging;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.http.HttpServletRequest;

class ApiRequestLoggingFilterTest {

    private final ApiRequestLoggingFilter filter = new ApiRequestLoggingFilter();

    @Test
    void logsRequestContextAndPropagatesValidRequestId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/items");
        request.addHeader("X-Request-Id", "client-request-123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (wrappedRequest, wrappedResponse) -> {
            assertThat(MDC.get("requestId")).isEqualTo("client-request-123");
            assertThat(((HttpServletRequest) wrappedRequest).getHeader("X-Request-Id"))
                    .isEqualTo("client-request-123");
            ((MockHttpServletResponse) wrappedResponse).setStatus(202);
        });

        assertThat(response.getHeader("X-Request-Id")).isEqualTo("client-request-123");
        assertThat(response.getStatus()).isEqualTo(202);
        assertThat(MDC.get("requestId")).isNull();
    }

    @Test
    void replacesInvalidRequestIdAndStillClearsMdc() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/items");
        request.addHeader("X-Request-Id", "bad\nrequest-id");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (wrappedRequest, wrappedResponse) -> {
            String requestId = ((HttpServletRequest) wrappedRequest).getHeader("X-Request-Id");
            assertThat(requestId).matches("[0-9a-f-]{36}");
            assertThat(MDC.get("requestId")).isEqualTo(requestId);
        });

        assertThat(response.getHeader("X-Request-Id")).matches("[0-9a-f-]{36}");
        assertThat(MDC.get("requestId")).isNull();
    }
}
