# Temples Service

## Purpose and API

Owns temple catalog records used by the website. Default port `9093` (`TEMPLES_PORT`); catalog base `/api/catalog/temples` supports public reads and role-protected create, update, and delete. The image resolver is `GET /api/catalog/temples/resolve-image`.

## Data and integrations

- Owns the existing `pddh_temples` database (`TEMPLES_DB_URL`); does not create the database.
- Uses the shared JWT and gateway internal-token checks for protected operations.
- Catalog writes require an idempotency key. Image fields accept public image URLs or local uploaded assets, as supported by the frontend.

## Run and build

Start Eureka and the gateway as needed. Build from `backend/` with `.\auth-service\mvnw.cmd -f pom.xml -pl temples-service -am test`. See [central backend reference](../README.md).
