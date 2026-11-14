# Gallery Service

## Purpose and API

Owns gallery content records. Default port `9097` (`GALLERY_PORT`); `/api/catalog/gallery` supports public reads and role-protected create, update, and delete.

## Data and integrations

- Owns the existing `pddh_gallery` database (`GALLERY_DB_URL`); does not create the database.
- Uses shared JWT role authorization and gateway internal-token validation.
- Catalog writes use idempotency keys and are normally routed through the API Gateway.

## Run and build

Build from `backend/` with `.\auth-service\mvnw.cmd -f pom.xml -pl gallery-service -am test`. See [central backend reference](../README.md).
