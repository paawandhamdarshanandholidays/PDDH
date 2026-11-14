# Customer Service

## Purpose

Provides customer dashboard functions and customer-specific profile and authentication endpoints. The CUSTOMER role is stored in the `user` table.

## Runtime and APIs

- Default port: `9114` (`CUSTOMER_SERVICE_PORT`).
- Dashboard: `/api/roles/customer/dashboard`; role profiles: `/api/roles/customer/profiles`.
- Internal role authentication: `POST /internal/authenticate`.

## Data and messaging

- Uses the existing `pddh_auth_service` database and `user` table for CUSTOMER-specific credential/profile lookups.
- Consumes queue `pddh.profile.customer` for profile synchronization.
- Shares the generic role profile implementation from `role-profile-common`; configuration selects CUSTOMER and table `user`.

## Configuration and build

Set `AUTH_DB_URL`, `JWT_SECRET`, `APP_GATEWAY_INTERNAL_TOKEN`, `EUREKA_URL`, and Kafka bootstrap server (`KAFKA_BOOTSTRAP_SERVERS`). Build from `backend/` using `.\auth-service\mvnw.cmd -f pom.xml -pl customer-service -am test`. See [central backend reference](../README.md).
