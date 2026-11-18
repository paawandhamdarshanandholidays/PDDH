package com.pddh.logging;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.servlet.http.HttpServletResponse;

class ApiRequestLoggingFilterTest {
    @Test
    void logsRequestStartAndOutcomeWithCorrelationId() throws Exception {
        Logger logger = (Logger) LoggerFactory.getLogger(ApiRequestLoggingFilter.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/catalog/gallery");
            request.addHeader("X-Request-Id", "request-123");
            MockHttpServletResponse response = new MockHttpServletResponse();

            new ApiRequestLoggingFilter().doFilterInternal(request, response, (wrapped, outgoing) ->
                    ((HttpServletResponse) outgoing).setStatus(HttpServletResponse.SC_CREATED));

            assertThat(response.getHeader("X-Request-Id")).isEqualTo("request-123");
            assertThat(appender.list).hasSize(2);
            assertThat(appender.list.get(0).getFormattedMessage())
                    .isEqualTo("API request started method=POST path=/api/catalog/gallery");
            assertThat(appender.list.get(1).getFormattedMessage())
                    .startsWith("API request completed method=POST path=/api/catalog/gallery status=201 durationMs=");
            assertThat(appender.list).allSatisfy(event ->
                    assertThat(event.getMDCPropertyMap()).containsEntry("requestId", "request-123"));
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    @Test
    void raisesLogLevelForClientAndServerFailures() throws Exception {
        Logger logger = (Logger) LoggerFactory.getLogger(ApiRequestLoggingFilter.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            logStatus(HttpServletResponse.SC_BAD_REQUEST);
            logStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

            assertThat(appender.list).hasSize(4);
            assertThat(appender.list.get(1).getLevel()).isEqualTo(ch.qos.logback.classic.Level.WARN);
            assertThat(appender.list.get(3).getLevel()).isEqualTo(ch.qos.logback.classic.Level.ERROR);
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    private void logStatus(int status) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/catalog/gallery");
        MockHttpServletResponse response = new MockHttpServletResponse();
        new ApiRequestLoggingFilter().doFilterInternal(request, response, (wrapped, outgoing) ->
                ((HttpServletResponse) outgoing).setStatus(status));
    }
}
