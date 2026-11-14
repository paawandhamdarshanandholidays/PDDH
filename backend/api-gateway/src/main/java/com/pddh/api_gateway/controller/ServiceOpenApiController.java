package com.pddh.api_gateway.controller;

import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api-docs/services")
public class ServiceOpenApiController {
    private static final Set<String> DOCUMENTED_SERVICES = Set.of(
            "auth-service",
            "booking-service",
            "ceo-service",
            "admin-service",
            "manager-service",
            "staff-service",
            "customer-service",
            "temples-service",
            "places-service",
            "events-service",
            "offers-service",
            "gallery-service",
            "contact-service",
            "trips-service",
            "stays-service");

    private final DiscoveryClient discoveryClient;
    private final RestClient restClient;
    private final String internalGatewayToken;

    public ServiceOpenApiController(
            DiscoveryClient discoveryClient,
            RestClient.Builder restClientBuilder,
            @Value("${app.gateway.internal-token}") String internalGatewayToken) {
        this.discoveryClient = discoveryClient;
        this.restClient = restClientBuilder.build();
        this.internalGatewayToken = internalGatewayToken;
    }

    @GetMapping("/{serviceId}")
    public ResponseEntity<JsonNode> getServiceOpenApi(
            @PathVariable String serviceId, HttpServletRequest request) {
        if (!DOCUMENTED_SERVICES.contains(serviceId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown API documentation service");
        }

        List<ServiceInstance> instances = discoveryClient.getInstances(serviceId);
        if (instances.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE, "The requested service is not currently registered");
        }

        JsonNode openApi = restClient.get()
                .uri(instances.getFirst().getUri().resolve("/v3/api-docs"))
                .header("X-Gateway-Request", "true")
                .header("X-Internal-Gateway-Token", internalGatewayToken)
                .retrieve()
                .body(JsonNode.class);
        if (openApi == null || !openApi.isObject()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "The service returned an invalid OpenAPI document");
        }

        ObjectNode document = (ObjectNode) openApi;
        ArrayNode servers = document.putArray("servers");
        servers.addObject().put("url", gatewayRootUrl(request));
        return ResponseEntity.ok(document);
    }

    private String gatewayRootUrl(HttpServletRequest request) {
        String scheme = request.getScheme();
        String host = request.getServerName();
        int port = request.getServerPort();
        boolean defaultPort = ("http".equalsIgnoreCase(scheme) && port == 80)
                || ("https".equalsIgnoreCase(scheme) && port == 443);
        return scheme + "://" + host + (defaultPort ? "" : ":" + port);
    }
}
