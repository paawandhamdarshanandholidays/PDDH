package com.pddh.auth_service.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

import com.pddh.auth_service.repository.UserRepository;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.jsonwebtoken.Claims;

class CurrentUserServiceTest {
    @Test
    void logsAccountLookupRejectionWithoutLoggingIdentityOrToken() {
        JwtService jwtService = mock(JwtService.class);
        UserRepository userRepository = mock(UserRepository.class);
        Claims claims = mock(Claims.class);
        when(jwtService.parseClaims("secret-token")).thenReturn(claims);
        when(claims.getSubject()).thenReturn("member@example.test");
        when(claims.get("role", String.class)).thenReturn("CUSTOMER");
        when(userRepository.findByUsernameIgnoreCaseAndRoleIgnoreCase("member@example.test", "CUSTOMER"))
                .thenReturn(Optional.empty());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users/me");
        request.addHeader("Authorization", "Bearer secret-token");

        Logger logger = (Logger) LoggerFactory.getLogger(CurrentUserService.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            ResponseStatusException error = assertThrows(
                    ResponseStatusException.class,
                    () -> new CurrentUserService(jwtService, userRepository).requireUser(request));

            assertThat(error.getStatusCode().value()).isEqualTo(401);
            assertThat(appender.list).singleElement().satisfies(event -> {
                assertThat(event.getFormattedMessage()).contains("reason=account_missing_or_role_changed");
                assertThat(event.getFormattedMessage()).doesNotContain("member@example.test");
                assertThat(event.getFormattedMessage()).doesNotContain("secret-token");
            });
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }
}
