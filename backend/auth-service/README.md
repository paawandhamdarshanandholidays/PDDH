# Auth Service

## Purpose

Owns registration, login, JWT issuance, user administration, password recovery, and synchronization of complete role profiles.

## Runtime and APIs

- Default port: `9092` (`AUTH_PORT` in test, QA, and production profiles).
- Authentication endpoints: `/auth/login`, `/auth/register`, `/auth/register-ceo`, `/auth/username-availability`, and `/auth/password-reset/**`.
- User management endpoints: `/api/users/**`.
- Auth-service calls `/internal/authenticate` on the selected role service for role-specific credential checks.

## Data and messaging

- Uses the existing `pddh_auth_service` PostgreSQL database. `combined_roles` holds aggregate account data; role records are also written to `users`, `admin`, `manager`, `ceo`, or `staff`.
- Email, username, and mobile uniqueness are enforced within a role, so the same details may be used for a different role. The username-availability endpoint checks email, suggested username, and any supplied mobile number together, and appends a numeric suffix when the preferred username is already taken in that role.
- `AUTH_DB_URL` and Kafka configuration are read from the application YAML.
- Profile-sync messages are published asynchronously through the configured Kafka topic.

## Operations

- Set `JWT_SECRET`, `AUTH_DB_URL`, Kafka bootstrap settings, and SMTP settings for password recovery.
- Auth file logs use the centralized external layout: `~/PDDH_Logs/auth-service_logs/auth-service_log.txt` locally, or `/var/log/pawandhamdarshanandholidays_log/auth-service_logs/auth-service_log.txt` in Docker, with daily/100 MB archives in its `archives/` subdirectory.
- Hibernate creates and updates mapped tables from JPA entities in the existing database; no runtime schema SQL file is required.

Build: `.\auth-service\mvnw.cmd -f pom.xml -pl auth-service -am test` from `backend/`. See [central backend reference](../README.md).
