# API Gateway

## Purpose

Single public HTTP entry point for the frontend. It routes requests to backend services through Eureka, applies gateway authentication and request policies, and adds the internal service token to downstream calls.

## Runtime and APIs

- Default port: `9091` (`GATEWAY_PORT` in non-default profiles).
- Public routes include `/auth/**`, `/api/users/**`, `/api/bookings/**`, `/api/catalog/**`, `/api/enquiries/**`, and `/api/roles/**`; route definitions live in `src/main/resources/application*.yaml`.
- Eureka at `8761` and downstream service ports must remain private.

## Dependencies and configuration

- Requires Eureka and the downstream services used by enabled routes.
- Set `EUREKA_URL`, `JWT_SECRET`, `APP_GATEWAY_INTERNAL_TOKEN`, and the target service URLs when not using local defaults.
- `APP_GATEWAY_INTERNAL_TOKEN` must match each downstream service and be at least 32 bytes.
- CORS and route security are configured in the application YAML files.

## Build and related docs

From `backend/`, run `.\auth-service\mvnw.cmd -f pom.xml -pl api-gateway -am test`. See [central backend reference](../README.md).
