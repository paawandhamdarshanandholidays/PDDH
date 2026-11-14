# Stays Service

## Purpose and API

Owns stay/hotel catalog records and validates stay bookings during the booking saga. Default port `9101` (`STAYS_PORT`); catalog API `/api/catalog/hotels` supports public reads and role-protected writes.

## Data and messaging

- Owns the existing `pddh_stays` database (`STAYS_DB_URL`); does not create the database.
- Consumes stay booking-created events from `pddh.booking.saga` on its durable queue, validates the stay, and publishes a validation result for booking-service.
- Requires Kafka and shared JWT/gateway-token configuration.

## Run and build

Build from `backend/` with `.\auth-service\mvnw.cmd -f pom.xml -pl stays-service -am test`. See [booking choreography](../README.md#booking-choreography).
