# Cloud Run deployment

Deploy one Spring Boot service from the `backend` directory. It listens on Cloud Run's `PORT` environment variable (9091 locally) and serves all API routes directly; Eureka, the API Gateway, Kafka, and service-to-service URLs are not required.

Build the executable JAR with:

```bash
./mvnw -B clean package
```

Set `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, and `JWT_SECRET` using Cloud Run environment configuration or Secret Manager. Point `DB_URL` at the existing `pddh` database, and retain the other microservice databases unchanged. Set `CORS_ALLOWED_ORIGINS` to the deployed frontend origin. Never commit credentials or signing keys.

Build the container from `backend/` after packaging the JAR. Configure the frontend's runtime `PDDH_API_URL` to the deployed monolith URL.
