# Manager Service

## Purpose

Provides manager dashboard functions and the MANAGER-specific profile and authentication endpoints.

## Runtime and APIs

- Default port: `9112` (`MANAGER_SERVICE_PORT`).
- Dashboard: `/api/roles/manager/dashboard`; role profiles: `/api/roles/manager/profiles`.
- Internal role authentication: `POST /internal/authenticate`.

## Data and messaging

- Uses the existing `pddh_auth_service` database and the `manager` table for manager-specific credential/profile lookups.
- Consumes queue `pddh.profile.manager` for profile synchronization.
- Shares the generic role profile implementation from `role-profile-common`; this service’s configuration selects MANAGER and `manager`.

## Configuration and build

Set `AUTH_DB_URL`, `JWT_SECRET`, `APP_GATEWAY_INTERNAL_TOKEN`, `EUREKA_URL`, and Kafka bootstrap server (`KAFKA_BOOTSTRAP_SERVERS`). Build from `backend/` using `.\auth-service\mvnw.cmd -f pom.xml -pl manager-service -am test`. See [central backend reference](../README.md).
