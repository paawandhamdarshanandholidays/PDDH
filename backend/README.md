# Pawan Dham backend microservices

This is the central backend reference for the microservices: architecture, service addresses, shared configuration, database ownership, messaging, security, operations, and local development. Each runnable service directory also contains its own `README.md` as the working record for that service. Update both the service document and this central reference when its APIs, database, events, environment variables, or run instructions change.

Each service directory is an independently runnable Spring Boot application. The five role services share the `role-profile-common` library for role profile listing and internal authentication; each selects its own role and table through service configuration. Catalog CRUD remains in each catalog service. Trips and stays validate booking events from their own catalog databases and return results through the booking saga.

## Services

| Service | Default port | Gateway API |
| --- | ---: | --- |
| [eureka-server](eureka-server/README.md) | 8761 | Eureka registry (internal only) |
| [api-gateway](api-gateway/README.md) | 9091 | Public API entry point |
| [auth-service](auth-service/README.md) | 9092 | `/auth`, `/api/users` |
| [temples-service](temples-service/README.md) | 9093 | `/api/catalog/temples` |
| [places-service](places-service/README.md) | 9094 | `/api/catalog/places` |
| [events-service](events-service/README.md) | 9095 | `/api/catalog/events` |
| [offers-service](offers-service/README.md) | 9096 | `/api/catalog/offers` |
| [gallery-service](gallery-service/README.md) | 9097 | `/api/catalog/gallery` |
| [contact-service](contact-service/README.md) | 9099 | `/api/catalog/contact`, `/api/enquiries` |
| [trips-service](trips-service/README.md) | 9100 | `/api/catalog/trips` |
| [stays-service](stays-service/README.md) | 9101 | `/api/catalog/hotels` |
| [booking-service](booking-service/README.md) | 9102 | `/api/bookings` |
| [ceo-service](ceo-service/README.md) | 9110 | `/api/roles/ceo/dashboard` |
| [admin-service](admin-service/README.md) | 9111 | `/api/roles/admin/dashboard` |
| [manager-service](manager-service/README.md) | 9112 | `/api/roles/manager/dashboard` |
| [staff-service](staff-service/README.md) | 9113 | `/api/roles/staff/dashboard` |
| [customer-service](customer-service/README.md) | 9114 | `/api/roles/customer/dashboard` |

Service-specific operational notes: [auth-service](auth-service/README.md), [API Gateway](api-gateway/README.md), [booking-service](booking-service/README.md), [Eureka](eureka-server/README.md), and each [role service](#role-profile-storage-and-access). Catalog and workflow notes are in the corresponding service directories.

`auth-service` issues JWTs and stores every account in `combined_roles` in the already-provisioned `pddh_auth_service` database. Each role also has its own JPA-mapped profile table in that database: `users`, `admin`, `manager`, `ceo`, and `staff`. Each profile table stores the canonical role (`CUSTOMER`, `ADMIN`, `MANAGER`, `CEO`, or `STAFF`) in its `role` column; startup compatibility migration adds and backfills the column for existing records. Hibernate creates and updates these tables from JPA entities at startup; applications do not load schema SQL files. Registration and profile changes write the full profile to `combined_roles` and the matching role table in one auth transaction. Profile events are published asynchronously through Kafka. Login verifies credentials and active status directly from the selected role table and updates `last_login_at` in both that table and `combined_roles`. People & Access shows each profile's active status and last login.

In the `dev` profile, auth-service and all role services log Hibernate SQL statements to their normal console and service log files. Set `SQL_LOG_LEVEL=INFO` to quiet SQL output. These settings log SQL statements without enabling parameter bind logging.

The five role services import the shared [`role-profile-common` module](role-profile-common/README.md). It supplies one JPA-backed role profile controller, repository, and service; each service selects its own role table and endpoint from its existing `app.profile-sync.table`, `app.role.expected`, and `app.role.path` settings.

## Role profile storage and access

The role-service documents describe the routes, status, Kafka profile-sync, and JWT behavior for each role. All five roles use the existing `pddh_auth_service` database with separate `users`, `admin`, `manager`, `ceo`, and `staff` tables. `customer-service` represents the `CUSTOMER` role and uses the `users` table. Auth-service maintains the combined `combined_roles` table alongside the role-specific records.

## JPA schema management

All application tables are mapped by JPA entities and created or updated by Hibernate using `spring.jpa.hibernate.ddl-auto=update`; application startup does not load or depend on separate SQL schema files. The obsolete `database-schemas/` scripts have been removed to avoid implying otherwise. Provision the PostgreSQL databases themselves before starting the services: Hibernate manages tables inside a connected database but does not create the database. The five role services share `AUTH_DB_URL`; other services use their existing `{SERVICE}_DB_URL` variables.

The backend Docker image caps the Java heap at 512 MB (`-Xms64m -Xmx512m`) to prevent a service from consuming unbounded container memory. Override `JAVA_TOOL_OPTIONS` when a service needs a different heap or when deploying with explicit container memory limits. Developer Tools are intentionally not inherited by production service modules.

Expose only the API gateway to public clients. Keep Eureka and all service ports on a private network. Downstream APIs require the shared `APP_GATEWAY_INTERNAL_TOKEN` (at least 32 bytes); every configured Gateway MVC route overwrites the internal request header before forwarding, and a direct API request without the token is rejected. The header is configured on each route because Gateway MVC does not support the WebFlux `default-filters` property. Set the same randomly generated value for the gateway and every downstream service.

Catalog services expose public `GET`, and role-protected `POST`, `PUT`, and `DELETE` operations. CEOs, administrators, and managers may write catalog data. The booking API accepts public trip/stay requests, while booking review requires a staff-or-higher JWT. Contact-service owns both contact catalog entries and enquiry submissions; listing enquiries and changing their status requires a staff-or-higher JWT.

## Booking choreography

Booking creation persists the booking and a `booking.created` message in the same database transaction through the outbox. On startup, booking-service adds a nullable `customer_id` column to older `bookings` tables if it is missing; existing rows remain unassigned, while new bookings store the authenticated customer username. The service publishes pending outbox records to Kafka topics with Resilience4j retry and delayed re-attempts. Trips and stays consume the created topic in separate consumer groups, validate matching booking types against their own catalog database, and publish to `pddh.booking.validation-results`. Booking consumes the result and confirms or rejects the pending booking. This is a choreography saga; there is no central saga orchestrator.

Kafka is required by auth/profile services and the booking, trips, and stays saga. Configure `KAFKA_BOOTSTRAP_SERVERS` for each participating service. Local IDE runs default to `localhost:9092`; the backend Docker image defaults to `host.docker.internal:9092` for a broker exposed on the Windows host. Override that value with the broker's reachable address when using a Docker network or Compose. Topics are created from Spring Kafka configuration when broker permissions allow it.

## Local configuration and monitoring

Every runnable service defaults to the `dev` Spring profile. Set `SPRING_PROFILES_ACTIVE` in each service process/container to `dev`, `test`, `qa`, or `prod` for the matching environment; the environment variable overrides the default. Profile-specific `application-{profile}.yaml` overrides are loaded where provided, and all services continue to inherit common settings from `application.yaml`. The dev profile supplies shared local-only fallback JWT and gateway-token values so services can start without those environment variables; set `JWT_SECRET` and `APP_GATEWAY_INTERNAL_TOKEN` to override them. Never use these fallback values outside local development.

For local development, the service profiles default `EUREKA_URL` to `http://localhost:8761/eureka/`; start `eureka-server` before the other services. Database-backed services default their local database credentials to `postgres`/`postgres`; override `DB_USERNAME` and `DB_PASSWORD` if your local PostgreSQL credentials differ. For test, QA, and production environments, set `JWT_SECRET` to the same random secret (at least 32 bytes) for auth-service, the gateway, booking-service, and all role/content services. Set `EUREKA_URL`, `APP_GATEWAY_INTERNAL_TOKEN` (at least 32 bytes), `DB_USERNAME`, and `DB_PASSWORD` in the environment of all applicable services. All five role services must share `AUTH_DB_URL`, pointing to the already-provisioned `pddh_auth_service` database. Content services accept `{SERVICE}_DB_URL` and `JPA_DDL_AUTO`; booking-service accepts `BOOKING_DB_URL`. Do not use local development credentials in deployed environments. Ensure environment variables contain actual credentials, not unresolved strings such as `${DB_USERNAME}` or `${DB_PASSWORD}`.

Before upgrading an environment that already has enquiry data, migrate its `enquiries` rows and `enquiry.submit` entries from `idempotency_records` into the contact-service database configured by `CONTACT_DB_URL`. Preserve those idempotency records so retries issued before the deployment cannot create duplicate enquiries after cutover.

Auth-service uses JPA entity mappings and Hibernate schema update at startup. A compatibility runner copies existing `users_data` and legacy role-table data into the current mapped tables when present; a Java-based role schema compatibility step backfills existing identity/password columns before applying their required constraints. No schema SQL resource is required. Existing data upgrades should be backed up before deployment.

Auth-service stores new passwords as BCrypt hashes. On the first successful login only, an existing account whose stored value is legacy plaintext is verified using a constant-time comparison and immediately upgraded to BCrypt; a wrong password is never upgraded. Existing bcrypt-backed accounts continue through the normal BCrypt verification path.

The gateway uses JWT bearer authentication only; it does not provide Basic Auth credentials. Gateway API requests receive the internal token header when routed. Do not publish downstream service ports or Eureka to the public network.

Set `EUREKA_URL` to the Eureka server URL for service discovery. Default ports can be overridden using the service-specific port variables shown in the table (for example `OFFERS_PORT` and `BOOKING_PORT`).

Each service exposes Actuator health, metrics, and Prometheus endpoints at `/actuator/health`, `/actuator/metrics`, and `/actuator/prometheus`; keep service and Actuator ports private. Error responses use the shared JSON envelope with timestamp, HTTP status, message, request path, and optional field-validation errors. Create/submission endpoints for managed accounts, bookings, enquiries, and catalog content require an `Idempotency-Key` request header (8–128 visible ASCII characters); Angular creates a secure key per payload and reuses it for retries. Public customer registration instead derives its idempotency key from the normalized, unique email address and fingerprints the normalized email with the request, so clients do not need to send that header. Each owning service persists a request fingerprint and response in its own database in the same transaction as the write. Repeating a key with the same payload replays the original response without repeating the operation; using the key with a different payload returns HTTP 409. Keys are scoped to the public submitter identity or authenticated content manager, and scope values are HMAC-SHA-256 protected. Set `APP_IDEMPOTENCY_SCOPE_SECRET` to a stable secret if idempotency keys must remain replayable across JWT secret rotation; otherwise the service uses its JWT secret. Login, reads, and naturally idempotent PUT/PATCH/DELETE operations do not require a key. The gateway CORS policy allows this header.

Write and submission endpoints apply a Resilience4j rate limiter (60 requests per operation per minute); Kafka publishing retries up to three attempts, then persists a delayed outbox retry.

The API gateway serves Swagger UI at `/swagger-ui/index.html`. Its API selector aggregates the OpenAPI definitions for all backend services registered in Eureka; each service definition is fetched through the gateway with the internal service token. Service APIs also expose their OpenAPI JSON at `/v3/api-docs` for gateway-mediated use.

All runnable backend services inherit one Logback configuration from [`shared-logging/src/main/resources/logback-spring.xml`](shared-logging/src/main/resources/logback-spring.xml). The backend reactor packages it into its services; the standalone Eureka build copies the same file explicitly. Service source folders do not maintain separate Logback configurations. With the standard Maven/IDE module working directory, each service writes files to `../../../PDDH_Logs/<spring.application.name>_logs/`, a sibling of the `PDDH` repository, and prints log lines to the console. Set `PAWANDHAM_LOG_ROOT` to an absolute path when launching from another working directory; it must be writable by the service process. Docker sets that variable to `/var/log/pawandhamdarshanandholidays_log`, which should be bind-mounted to a host directory if logs need to persist outside the container. The active file is `<spring.application.name>_log.txt`; archives go in `archives/` and use `<spring.application.name>_log_DD-MM-YYYY_HHmmss_N.txt`. Logback rolls at the start of a new day or when an active file reaches 100 MB, uses timestamped archive names with a rollover index to prevent collisions, and retains 30 days of archives. Log lines use `yyyy-MM-dd HH:mm:ss.SSS LEVEL [thread] logger - message`.

The backend Dockerfile creates `/var/log/pawandhamdarshanandholidays_log` for container user `10001`, sets `PAWANDHAM_LOG_ROOT` to that path, and declares it as a volume. To persist files outside the container, bind-mount the host directory to the same absolute path for every service container; make the host directory writable by UID/GID `10001` (for example, `sudo install -d -m 0750 -o 10001 -g 10001 /var/log/pawandhamdarshanandholidays_log`). Do not mount a directory inside the source tree.

Native/AOT builds disable Spring Cloud refresh scope during AOT processing (`spring.cloud.refresh.enabled=false`) because refresh-scoped Eureka clients are not supported by Spring AOT; Eureka configuration is startup-only in the generated image.

Build all backend services from the backend root with Maven:

```powershell
cd backend
.\auth-service\mvnw.cmd -f pom.xml test
```

Customer registration is available at `/register`. The first CEO can be created through `/ceo/register` during bootstrap; additional CEO and other staff/admin/manager profiles must be created by an authorized user in People & Access (`/portal/users`). Auth-service supports CEO registration at `POST /auth/register-ceo`, and later CEO registration through that endpoint requires an authenticated CEO. Customer password recovery uses `POST /auth/password-reset/otp/request` and `POST /auth/password-reset/otp/confirm`; configure `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD`, and `SMTP_FROM` in auth-service to deliver codes. Codes expire after 10 minutes and are invalidated after five failed attempts. Customers can also change their own password while signed in at `POST /api/users/me/password`, after supplying the current password.

Admin and CEO profile browsing is lazy-loaded by role through `GET /api/users?role={CUSTOMER|STAFF|MANAGER|ADMIN}&page=0&size=50`. The API caps every response at 50 profiles; the profile-management page fetches additional pages sequentially when a larger total is requested. Admins can view and create admin profiles, as well as manage manager, staff, and customer profiles.

Staff, managers, administrators, and CEOs can submit their own password reset request through `POST /api/users/password-reset-requests`. Administrators and CEOs can submit a reset request for another account by including its `targetUserId`. Requests do not take effect until a CEO reviews them through `GET /api/users/password-reset-requests` and `PATCH /api/users/password-reset-requests/{id}?approve=true|false`; requesters cannot approve their own requests. The customer recovery page is available at `/forgot-password`, and signed-in users can reset or request a reset from their profile.
