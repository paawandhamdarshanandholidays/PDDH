package com.pddh.staff_service.api_common.resilience;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.micrometer.core.instrument.MeterRegistry;

@Component
public class RateLimitGuard {
    private final RateLimiterRegistry registry;
    private final MeterRegistry meterRegistry;

    public RateLimitGuard(RateLimiterRegistry registry, MeterRegistry meterRegistry) {
        this.registry = registry;
        this.meterRegistry = meterRegistry;
    }

    public void check(String operation) {
        if (!registry.rateLimiter(operation).acquirePermission()) {
            meterRegistry.counter("pddh.rate_limiter.rejected", "operation", operation).increment();
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many requests. Please try again shortly.");
        }
    }
}
