# PDDH Backend

The backend is one Spring Boot application, built as one executable JAR and served on port `9091`. Controllers, business services, repositories, and persistence entities from the former services are compiled directly in this project. Business workflows use in-process Spring service calls; Kafka, Eureka, and the API Gateway are not part of the application.

## Local development

Requirements: JDK 25, PostgreSQL, and PowerShell.

The application uses the existing PostgreSQL database `pddh`. Existing per-microservice databases are left untouched. From this directory run:

```powershell
.\mvnw.cmd package
.\run-backend.ps1
```

## Import into Eclipse

Use an Eclipse workspace folder separate from this project directory. Choose **File > Import > Maven > Existing Maven Projects**, set the root directory to the repository's `backend` folder, and select `backend/pom.xml`. Configure a JDK 25 under **Preferences > Java > Installed JREs**, then run **Maven > Update Project** after import.

The Eclipse project name is `pddh-backend` to avoid collisions with a generic `backend` project. Its project metadata declares both Java and Maven natures; the POM remains the source of dependency and compiler settings.

By default the application connects to `jdbc:postgresql://localhost:5432/pddh` as `postgres` / `postgres` and listens on `http://localhost:9091`. Configure these environment variables as needed:

| Variable | Default | Purpose |
| --- | --- | --- |
| `SERVER_PORT` | `9091` | HTTP port |
| `DB_URL` | `jdbc:postgresql://localhost:5432/pddh` | PostgreSQL JDBC URL |
| `DB_USERNAME` / `DB_PASSWORD` | `postgres` | Database credentials |
| `JWT_SECRET` | Local-development-only value | JWT signing secret; set a unique secret outside local development |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:4200` | Comma-separated frontend origins |
| `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD`, `SMTP_FROM` | Empty/default SMTP settings | Password-reset email delivery |

Hibernate updates tables at startup. Existing per-service databases are not migrated, modified, or deleted by the monolith.

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
.\mvnw.cmd package
docker build -t pddh-backend -f Dockerfile .
```

Configure `DB_URL`, database credentials, and a production `JWT_SECRET` for the container, then publish only port `9091`.
