# Trips Service

## Purpose and API

Owns trip catalog records and validates trip bookings during the booking saga. Default port `9100` (`TRIPS_PORT`); catalog API `/api/catalog/trips` supports public reads and role-protected writes.

## Data and messaging

- Owns the existing `pddh_trips` database (`TRIPS_DB_URL`); does not create the database.
- Consumes trip booking-created events from `pddh.booking.saga` on its durable queue, validates the trip, and publishes a validation result for booking-service.
- Requires Kafka and shared JWT/gateway-token configuration.

## Run and build

Build from `backend/` with `.\auth-service\mvnw.cmd -f pom.xml -pl trips-service -am test`. See [booking choreography](../README.md#booking-choreography).
