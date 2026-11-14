# Shared Role Profile Library

This module is a shared implementation library, not a separately deployed microservice. The five role services import it to avoid duplicating role profile behavior.

## Supplied components

- One `RoleProfileController` exposes each service's configured role-profile route and internal authentication endpoint.
- One JPA `RoleProfileRepository` accesses the configured role table.
- One `RoleProfileService` applies role access rules, performs BCrypt checks, records last login, and synchronizes role profiles.
- `ProfileSyncListener` consumes the queue configured by the importing service.

## Role selection

The importing service supplies `app.profile-sync.table`, `app.role.expected`, `app.role.path`, and `app.profile-sync.topic`. Services point to the existing `pddh_auth_service` database and separate tables (`users`, `admin`, `manager`, `ceo`, and `staff`). JPA entities in this module own role-table schema generation through Hibernate `ddl-auto`.

## Build

From `backend/`, compile it with a role service using `./auth-service/mvnw.cmd -f pom.xml -pl role-profile-common,admin-service -am -DskipTests compile`. See the [central backend reference](../README.md).
