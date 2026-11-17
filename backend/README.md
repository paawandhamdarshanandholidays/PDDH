# PDDH Backend

The backend is one Spring Boot application, built as one executable JAR and served on port `9091`. Controllers, business services, repositories, and persistence entities from the former services are compiled into `pddh-backend`. Business workflows use in-process Spring service calls; Kafka, Eureka, and the API Gateway are not part of the application.

## Local development

Requirements: JDK 25, PostgreSQL, and PowerShell.

Create a fresh PostgreSQL database named `pddh_monolith`, then from this directory run:

```powershell
.\mvnw.cmd -pl pddh-backend package
.\run-backend.ps1
```

By default the application connects to `jdbc:postgresql://localhost:5432/pddh_monolith` as `postgres` / `postgres` and listens on `http://localhost:9091`. Configure these environment variables as needed:

| Variable | Default | Purpose |
| --- | --- | --- |
| `SERVER_PORT` | `9091` | HTTP port |
| `DB_URL` | `jdbc:postgresql://localhost:5432/pddh_monolith` | PostgreSQL JDBC URL |
| `DB_USERNAME` / `DB_PASSWORD` | `postgres` | Database credentials |
| `JWT_SECRET` | Local-development-only value | JWT signing secret; set a unique secret outside local development |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:4200` | Comma-separated frontend origins |
| `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD`, `SMTP_FROM` | Empty/default SMTP settings | Password-reset email delivery |

Hibernate updates tables at startup. The monolith expects a fresh database; records in the previous per-service databases are not copied automatically.

## API

The existing frontend origin and API paths are retained. The Angular app defaults to `http://localhost:9091`; set `globalThis.PDDH_API_URL` to override it.

- Authentication and account management: `/auth` and `/api/users`
- Public catalog reads: `/api/catalog/{temples,places,events,offers,gallery,contact,trips,hotels}`
- Booking management: `/api/bookings`
- Role dashboards and profiles: `/api/roles/{ceo,admin,manager,staff,customer}`
- Enquiries: `/api/enquiries`
- Health: `/actuator/health`

Public catalog reads, login, registration, and enquiry submissions remain public. Other protected operations require a JWT bearer token.

## Container

Build the JAR first, then build the image from this directory:

```powershell
.\mvnw.cmd -pl pddh-backend package
docker build -t pddh-backend -f Dockerfile .
```

Configure `DB_URL`, database credentials, and a production `JWT_SECRET` for the container, then publish only port `9091`.
