# Booking Service

## Purpose

Accepts customer trip and stay bookings, exposes staff review operations, and coordinates catalog validation with the trips and stays services.

## Runtime and APIs

- Default port: `9102` (`BOOKING_PORT`).
- API base: `/api/bookings`; create and list bookings, and change status with `PATCH /api/bookings/{id}/status`.
- Write requests use idempotency keys. A signed-in customer may submit identical booking details only once per booking date; a repeat with a new key returns `409 Conflict`. Review operations are role-protected.

## Data and messaging

- Owns the existing `pddh_bookings` database (`BOOKING_DB_URL`).
- On startup, adds a nullable `customer_id` column when upgrading a `bookings` table that predates customer ownership. Existing rows remain unassigned because their account username cannot be inferred; new bookings always store the authenticated customer's username.
- Persists booking writes and outbox events in one transaction.
- Publishes `booking.created` to `pddh.booking.created`; consumes validation results from trips and stays and records the final booking state.
- Requires a reachable Kafka broker configured with `KAFKA_BOOTSTRAP_SERVERS` and permission to create the configured topics. Failed sends remain in the outbox and are retried.

## Operations

Run Eureka before the service. Keep its port private; expose the API through the gateway. Native/AOT builds disable Spring Cloud refresh scope during AOT processing, so Eureka configuration is startup-only in the generated image. Build with `.\auth-service\mvnw.cmd -f pom.xml -pl booking-service -am test` from `backend/`. See [central backend reference](../README.md).
