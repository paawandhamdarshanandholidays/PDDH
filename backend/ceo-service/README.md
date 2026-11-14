# CEO Service

## Purpose

Provides CEO dashboard functions and the CEO-specific profile and authentication endpoints.

## Runtime and APIs

- Default port: `9110` (`CEO_SERVICE_PORT`).
- Dashboard: `/api/roles/ceo/dashboard`; role profiles: `/api/roles/ceo/profiles`.
- Internal role authentication: `POST /internal/authenticate`.

## Data and messaging

- Uses the existing `pddh_auth_service` database and the `ceo` table for CEO-specific credential/profile lookups.
- Consumes queue `pddh.profile.ceo` for profile synchronization.
- Shares the generic role profile implementation from `role-profile-common`; this service’s configuration selects CEO and `ceo`.

## Configuration and build

Set `AUTH_DB_URL`, `JWT_SECRET`, `APP_GATEWAY_INTERNAL_TOKEN`, `EUREKA_URL`, and Kafka bootstrap server (`KAFKA_BOOTSTRAP_SERVERS`). Build from `backend/` using `.\auth-service\mvnw.cmd -f pom.xml -pl ceo-service -am test`. See [central backend reference](../README.md).
