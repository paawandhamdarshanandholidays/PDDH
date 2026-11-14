# Eureka Server

## Purpose

Provides service registration and discovery for the gateway and backend microservices.

## Runtime

- Default port: `8761` (`EUREKA_PORT` in non-default profiles).
- Registry UI and discovery endpoints are internal infrastructure; do not expose them publicly.
- Services use `EUREKA_URL`, defaulting to `http://localhost:8761/eureka/` for local development.

## Operations

Start before the gateway and service instances. No application database is owned by Eureka. Eureka packages the shared Logback configuration and writes to `../../../PDDH_Logs/eureka-server_logs/` from its standard module working directory, outside the repository. Build with `.\auth-service\mvnw.cmd -f pom.xml -pl eureka-server -am test` from `backend/`. See [central backend reference](../README.md).
