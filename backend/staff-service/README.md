# Staff Service

## Purpose

Provides staff dashboard functions and the STAFF-specific profile and authentication endpoints.

## Runtime and APIs

- Default port: `9113` (`STAFF_SERVICE_PORT`).
- Dashboard: `/api/roles/staff/dashboard`; role profiles: `/api/roles/staff/profiles`.
- Internal role authentication: `POST /internal/authenticate`.

## Data and messaging

- Uses the existing `pddh_auth_service` database and the `staff` table for staff-specific credential/profile lookups.
- Consumes queue `pddh.profile.staff` for profile synchronization.
- Shares the generic role profile implementation from `role-profile-common`; this service’s configuration selects STAFF and `staff`.

## Configuration and build

Set `AUTH_DB_URL`, `JWT_SECRET`, `APP_GATEWAY_INTERNAL_TOKEN`, `EUREKA_URL`, and Kafka bootstrap server (`KAFKA_BOOTSTRAP_SERVERS`). Build from `backend/` using `.\auth-service\mvnw.cmd -f pom.xml -pl staff-service -am test`. See [central backend reference](../README.md).
