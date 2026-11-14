# Places Service

## Purpose and API

Owns place catalog records. Default port `9094` (`PLACES_PORT`); `/api/catalog/places` supports public reads and role-protected create, update, and delete.

## Data and integrations

- Owns the existing `pddh_places` database (`PLACES_DB_URL`); does not create the database.
- Requires the shared JWT and `APP_GATEWAY_INTERNAL_TOKEN` for protected downstream requests.
- Writes use idempotency keys; catalog changes are served to the frontend through the API Gateway.

## Run and build

Build from `backend/` with `.\auth-service\mvnw.cmd -f pom.xml -pl places-service -am test`. See [central backend reference](../README.md).
