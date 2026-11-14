package com.pddh.auth_service.service;

import java.util.Locale;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;

import com.pddh.auth_service.dto.RoleAuthenticationRequest;
import com.pddh.auth_service.dto.RoleAuthenticationResponse;

import reactor.core.publisher.Mono;

@Service
public class RoleAuthenticationClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoleAuthenticationClient.class);
    private static final Map<String, String> SERVICES = Map.of(
            "CEO", "ceo-service",
            "ADMIN", "admin-service",
            "MANAGER", "manager-service",
            "STAFF", "staff-service",
            "CUSTOMER", "customer-service");

    private final WebClient client;
    private final String gatewayToken;

    public RoleAuthenticationClient(
            WebClient.Builder roleAuthWebClientBuilder,
            @Value("${app.gateway.internal-token}") String gatewayToken) {
        this.client = roleAuthWebClientBuilder.build();
        this.gatewayToken = gatewayToken;
    }

    public Mono<RoleAuthenticationResponse> authenticate(String role, String identifier, String password) {
        String normalizedRole = role.trim().toUpperCase(Locale.ROOT);
        String service = SERVICES.get(normalizedRole);
        if (service == null) {
            return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid role or credentials"));
        }
        return client.post()
                .uri("http://" + service + "/internal/authenticate")
                .header("X-Internal-Gateway-Token", gatewayToken)
                .bodyValue(new RoleAuthenticationRequest(identifier, password))
                .retrieve()
                .onStatus(status -> status.value() == 401,
                        response -> Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                                "Invalid username or password")))
                .onStatus(status -> status.isError(),
                        response -> {
                            LOGGER.warn("Role authentication service {} returned HTTP {}", service,
                                    response.statusCode().value());
                            return Mono.error(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                                    "Selected role authentication is temporarily unavailable"));
                        })
                .bodyToMono(RoleAuthenticationResponse.class)
                .timeout(java.time.Duration.ofSeconds(5))
                .onErrorMap(java.util.concurrent.TimeoutException.class,
                        exception -> new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                                "Selected role authentication timed out", exception))
                .onErrorMap(exception -> !(exception instanceof ResponseStatusException),
                        exception -> {
                            LOGGER.warn("Could not reach role authentication service {}", service, exception);
                            return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                                    "Selected role authentication is temporarily unavailable", exception);
                        });
    }
}
