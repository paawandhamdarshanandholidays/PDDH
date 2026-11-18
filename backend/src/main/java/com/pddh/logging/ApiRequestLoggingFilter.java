package com.pddh.logging;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;

class ApiRequestLoggingFilter extends OncePerRequestFilter {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApiRequestLoggingFilter.class);
    private static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final String REQUEST_ID_MDC_KEY = "requestId";
    private static final Pattern VALID_REQUEST_ID = Pattern.compile("[A-Za-z0-9._-]{1,64}");
    private static final Set<String> BODY_LOGGABLE_CATALOG_RESOURCES =
            Set.of("temples", "places", "events", "offers", "gallery", "hotels", "trips");
    private static final Set<String> SENSITIVE_FIELD_PARTS = Set.of(
            "password", "token", "secret", "authorization", "email", "phone", "mobile",
            "username", "otp", "image", "photo", "src", "address", "contact", "gender");
    private static final int MAX_BODY_CACHE_BYTES = 8 * 1024;
    private static final int MAX_BODY_LOG_CHARACTERS = 4 * 1024;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String requestId = resolveRequestId(request.getHeader(REQUEST_ID_HEADER));
        String previousRequestId = MDC.get(REQUEST_ID_MDC_KEY);
        HttpServletRequest wrappedRequest = new RequestIdRequestWrapper(request, requestId);
        boolean logBodies = isBodyLoggableCatalogPath(request.getRequestURI());
        ContentCachingRequestWrapper cachedRequest = logBodies
                ? new ContentCachingRequestWrapper(wrappedRequest, MAX_BODY_CACHE_BYTES)
                : null;
        ContentCachingResponseWrapper cachedResponse = logBodies
                ? new ContentCachingResponseWrapper(response)
                : null;
        long startTime = System.nanoTime();

        MDC.put(REQUEST_ID_MDC_KEY, requestId);
        response.setHeader(REQUEST_ID_HEADER, requestId);
        LOGGER.info("API request started method={} path={}", request.getMethod(), request.getRequestURI());
        try {
            filterChain.doFilter(
                    cachedRequest == null ? wrappedRequest : cachedRequest,
                    cachedResponse == null ? response : cachedResponse);
        } finally {
            try {
                if (cachedRequest != null && cachedResponse != null) {
                    logSafeBodies(request, response, cachedRequest, cachedResponse);
                }
                long durationMs = (System.nanoTime() - startTime) / 1_000_000;
                int status = response.getStatus();
                if (status >= HttpServletResponse.SC_INTERNAL_SERVER_ERROR) {
                    LOGGER.error("API request failed method={} path={} status={} durationMs={}",
                            request.getMethod(), request.getRequestURI(), status, durationMs);
                } else if (status >= HttpServletResponse.SC_BAD_REQUEST) {
                    LOGGER.warn("API request rejected method={} path={} status={} durationMs={}",
                            request.getMethod(), request.getRequestURI(), status, durationMs);
                } else {
                    LOGGER.info("API request completed method={} path={} status={} durationMs={}",
                            request.getMethod(), request.getRequestURI(), status, durationMs);
                }
            } finally {
                try {
                    if (cachedResponse != null) {
                        cachedResponse.copyBodyToResponse();
                    }
                } finally {
                    if (previousRequestId == null) {
                        MDC.remove(REQUEST_ID_MDC_KEY);
                    } else {
                        MDC.put(REQUEST_ID_MDC_KEY, previousRequestId);
                    }
                }
            }
        }
    }

    private void logSafeBodies(
            HttpServletRequest request,
            HttpServletResponse response,
            ContentCachingRequestWrapper cachedRequest,
            ContentCachingResponseWrapper cachedResponse) {
        byte[] requestBody = cachedRequest.getContentAsByteArray();
        byte[] responseBody = cachedResponse.getContentAsByteArray();
        if (requestBody.length == 0 && responseBody.length == 0) {
            return;
        }

        boolean requestTruncated = request.getContentLengthLong() > MAX_BODY_CACHE_BYTES
                || requestBody.length >= MAX_BODY_CACHE_BYTES;
        LOGGER.info("API payload method={} path={} requestBody={} responseBody={}",
                request.getMethod(),
                request.getRequestURI(),
                safeJsonBody(requestBody, request.getContentType(), requestTruncated),
                safeJsonBody(responseBody, response.getContentType(), false));
    }

    private static boolean isBodyLoggableCatalogPath(String path) {
        String[] segments = path.split("/");
        return segments.length >= 4
                && "api".equals(segments[1])
                && "catalog".equals(segments[2])
                && BODY_LOGGABLE_CATALOG_RESOURCES.contains(segments[3])
                && !path.endsWith("/image")
                && !path.contains("/resolve-image");
    }

    private static String safeJsonBody(byte[] body, String contentType, boolean truncated) {
        if (body.length == 0) {
            return "[empty]";
        }
        if (truncated) {
            return "[omitted: body exceeds logging limit]";
        }
        if (!isJsonContentType(contentType)) {
            return "[omitted: non-JSON body]";
        }
        try {
            JsonNode parsed = OBJECT_MAPPER.readTree(body);
            if (parsed == null) {
                return "[empty]";
            }
            String safeBody = OBJECT_MAPPER.writeValueAsString(redactSensitiveFields(parsed));
            if (safeBody.length() > MAX_BODY_LOG_CHARACTERS) {
                return safeBody.substring(0, MAX_BODY_LOG_CHARACTERS) + "...[truncated]";
            }
            return safeBody;
        } catch (IOException exception) {
            return "[omitted: invalid JSON body]";
        }
    }

    private static boolean isJsonContentType(String contentType) {
        if (contentType == null) {
            return false;
        }
        String normalized = contentType.toLowerCase(Locale.ROOT);
        return normalized.contains("application/json") || normalized.contains("+json");
    }

    private static JsonNode redactSensitiveFields(JsonNode value) {
        if (value.isObject()) {
            ObjectNode redacted = OBJECT_MAPPER.createObjectNode();
            value.fields().forEachRemaining(field -> redacted.set(
                    field.getKey(),
                    isSensitiveField(field.getKey())
                            ? OBJECT_MAPPER.getNodeFactory().textNode("[REDACTED]")
                            : redactSensitiveFields(field.getValue())));
            return redacted;
        }
        if (value.isArray()) {
            ArrayNode redacted = OBJECT_MAPPER.createArrayNode();
            value.forEach(item -> redacted.add(redactSensitiveFields(item)));
            return redacted;
        }
        if (value.isTextual() && value.asText().startsWith("data:")) {
            return OBJECT_MAPPER.getNodeFactory().textNode("[REDACTED]");
        }
        return value.deepCopy();
    }

    private static boolean isSensitiveField(String fieldName) {
        String normalized = fieldName.replaceAll("[^A-Za-z0-9]", "").toLowerCase(Locale.ROOT);
        return SENSITIVE_FIELD_PARTS.stream().anyMatch(normalized::contains);
    }

    private String resolveRequestId(String suppliedRequestId) {
        if (suppliedRequestId != null && VALID_REQUEST_ID.matcher(suppliedRequestId).matches()) {
            return suppliedRequestId;
        }
        return UUID.randomUUID().toString();
    }

    private static final class RequestIdRequestWrapper extends HttpServletRequestWrapper {
        private final String requestId;

        private RequestIdRequestWrapper(HttpServletRequest request, String requestId) {
            super(request);
            this.requestId = requestId;
        }

        @Override
        public String getHeader(String name) {
            if (REQUEST_ID_HEADER.equalsIgnoreCase(name)) {
                return requestId;
            }
            return super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            if (REQUEST_ID_HEADER.equalsIgnoreCase(name)) {
                return Collections.enumeration(List.of(requestId));
            }
            return super.getHeaders(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            List<String> headerNames = new ArrayList<>(Collections.list(super.getHeaderNames()));
            headerNames.removeIf(REQUEST_ID_HEADER::equalsIgnoreCase);
            headerNames.add(REQUEST_ID_HEADER);
            return Collections.enumeration(headerNames);
        }
    }
}
