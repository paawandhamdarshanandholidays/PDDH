# Admin Service

## Purpose

Provides administrator dashboard functions and the ADMIN-specific profile and authentication endpoints.

## Runtime and APIs

- Default port: `9111` (`ADMIN_SERVICE_PORT`).
- Dashboard: `/api/roles/admin/dashboard`; role profiles: `/api/roles/admin/profiles`.
- Internal role authentication: `POST /internal/authenticate`.

## Data and messaging

- Uses the existing `pddh_auth_service` database and only the `admin` table for ADMIN-specific credentials/profile lookups.
- Shares `combined_roles` account data and the `admin` profile table in `pddh_auth_service`; consumes Kafka topic `pddh.profile.admin`.
- Uses the shared `role-profile-common` controller, repository, and service; `app.role.expected` and `app.profile-sync.table` select ADMIN and `admin`.

## Configuration and build

Set `AUTH_DB_URL`, `JWT_SECRET`, `APP_GATEWAY_INTERNAL_TOKEN`, `EUREKA_URL`, and Kafka bootstrap server (`KAFKA_BOOTSTRAP_SERVERS`). Build from `backend/` using `.\auth-service\mvnw.cmd -f pom.xml -pl admin-service -am test`. See [central backend reference](../README.md).
