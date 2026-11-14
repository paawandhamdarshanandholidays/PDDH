# Contact Service

## Purpose and API

Owns contact catalog information and submitted enquiries. Default port `9099` (`CONTACT_PORT`). Catalog API: `/api/catalog/contact`; enquiry API: `/api/enquiries` supports submissions, staff-protected listing, and status updates.

## Data and integrations

- Owns the existing `pddh_contact` database (`CONTACT_DB_URL`); stores its contact and enquiry data and idempotency records.
- Enquiry submission is idempotent; enquiry review requires a staff-or-higher role.
- Uses the shared JWT and gateway internal-token checks.

## Run and build

Build from `backend/` with `.\auth-service\mvnw.cmd -f pom.xml -pl contact-service -am test`. Before migration, follow the enquiry cutover note in [central backend reference](../README.md).
