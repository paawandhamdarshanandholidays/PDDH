# Offers Service

## Purpose and API

Owns promotional offer catalog records. Default port `9096` (`OFFERS_PORT`); `/api/catalog/offers` supports public reads and role-protected create, update, and delete.

## Data and integrations

- Owns the existing `pddh_offers` database (`OFFERS_DB_URL`); does not create the database.
- Uses shared JWT role authorization and gateway internal-token validation.
- Catalog writes use idempotency keys and are normally routed through the API Gateway.

## Run and build

Build from `backend/` with `.\auth-service\mvnw.cmd -f pom.xml -pl offers-service -am test`. See [central backend reference](../README.md).
