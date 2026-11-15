# Cloud Run deployment configuration

The production Spring profile is `prod`. Set `SPRING_PROFILES_ACTIVE=prod` for each Cloud Run service. The production configs bind to Cloud Run's `PORT` (default 8080), disable Eureka, externalize PostgreSQL credentials, and cap HikariCP at three connections per container.

## Database and secrets

Set `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, and `SPRING_DATASOURCE_PASSWORD` on each backend service using Cloud Run secrets/environment configuration. Use the supplied Neon pooled endpoint and keep the database name `neondb` for every service; include `sslmode=require` in the JDBC URL. Never put database credentials, JWT keys, or the gateway token in source control. Rotate the database password because it was included in a chat message.

Set `JWT_SECRET` and `APP_GATEWAY_INTERNAL_TOKEN` consistently across the gateway and all microservices. These are required settings, not repository defaults.

## Kafka-dependent services

Authentication, booking, stays, trips, and role-profile services use Kafka-based profile sync or booking saga functionality. Configure `KAFKA_BOOTSTRAP_SERVERS` for those services with a reachable production broker; do not disable Kafka auto-configuration for them. The other catalog services do not require a broker for their current service code.

## Gateway and frontend

The gateway production profile reads each downstream URL from the corresponding `*_SERVICE_URL` variable (for example `AUTH_SERVICE_URL`, `BOOKING_SERVICE_URL`, and `ADMIN_SERVICE_URL`), and `FRONTEND_URL` should be the deployed Vercel origin. The Cloud Run gateway URL is not yet known, so do not bake a placeholder into Angular. The frontend reads the runtime global `PDDH_API_URL`; set it to the actual gateway URL in the Vercel runtime/deployment configuration when available.

Set the gateway and microservices to `SPRING_PROFILES_ACTIVE=prod`. Eureka remains in the repository for local development and is not required on Cloud Run. Deploy the microservices first, then configure the gateway with their HTTPS URLs, then point Vercel at the gateway URL.
