package com.pddh.logging;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.http.MediaType;

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

    @Test
    void logsRedactedJsonBodiesForPublicCatalogRequests() throws Exception {
        Logger logger = (Logger) LoggerFactory.getLogger(ApiRequestLoggingFilter.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/catalog/gallery");
            request.setContentType(MediaType.APPLICATION_JSON_VALUE);
            request.setContent("""
                    {"src":"data:image/png;base64,private-image-data","alt":"Temple","category":"Ayodhya","email":"private@example.test"}
                    """.getBytes(StandardCharsets.UTF_8));
            MockHttpServletResponse response = new MockHttpServletResponse();

            new ApiRequestLoggingFilter().doFilterInternal(request, response, (wrapped, outgoing) -> {
                ((jakarta.servlet.http.HttpServletRequest) wrapped).getInputStream().readAllBytes();
                HttpServletResponse httpResponse = (HttpServletResponse) outgoing;
                httpResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
                httpResponse.getWriter().write("""
                        {"id":"gallery-1","src":"/api/catalog/gallery/gallery-1/image","category":"Ayodhya"}
                        """);
            });

            assertThat(response.getContentAsString()).contains("gallery-1");
            assertThat(appender.list).anySatisfy(event -> {
                assertThat(event.getFormattedMessage()).contains("API payload");
                assertThat(event.getFormattedMessage()).contains("requestBody=");
                assertThat(event.getFormattedMessage()).contains("responseBody=");
                assertThat(event.getFormattedMessage()).contains("[REDACTED]");
                assertThat(event.getFormattedMessage()).doesNotContain("private-image-data");
                assertThat(event.getFormattedMessage()).doesNotContain("private@example.test");
            });
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    @Test
    void neverLogsProfileBodiesOrAuthorizationTokens() throws Exception {
        Logger logger = (Logger) LoggerFactory.getLogger(ApiRequestLoggingFilter.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users/me");
            request.addHeader("Authorization", "Bearer do-not-log-this-token");
            MockHttpServletResponse response = new MockHttpServletResponse();

            new ApiRequestLoggingFilter().doFilterInternal(request, response, (wrapped, outgoing) -> {
                HttpServletResponse httpResponse = (HttpServletResponse) outgoing;
                httpResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
                httpResponse.getWriter().write("{\"email\":\"private@example.test\"}");
            });

            List<String> messages = appender.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
            assertThat(messages).noneMatch(message -> message.contains("API payload"));
            assertThat(messages).noneMatch(message -> message.contains("do-not-log-this-token"));
            assertThat(messages).noneMatch(message -> message.contains("private@example.test"));
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
