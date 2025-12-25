# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build and Run Commands

### Server (Kotlin/Spring Boot)

```bash
# Build and run
mvn -pl server spring-boot:run

# Run with data import (from ../data directory)
mvn -pl server spring-boot:run -Dspring-boot.run.arguments="--heartrate --steps --calories"

# Import options: --heartrate, --steps, --calories, --distance, --exercise, --sleep,
# --sleepscore, --restingheartrate, --timeinzone, --activityminutes, --activezoneminutes,
# --vo2max, --runvo2max, --activitygoals, --devicetemperature, --respiratoryrate,
# --hrv, --hrvdetails, --minutespo2, --computedtemperature, --respiratoryratesummary, --dailyspo2

# Run tests
mvn -pl server test

# Run a single test
mvn -pl server test -Dtest=HeartRateImporterImplTest

# Compile only
mvn -pl server compile
```

### Client (React/TypeScript)

```bash
cd client
npm start    # Dev server on port 3000, proxies to :8080
npm test     # Run tests
npm run build
```

### Infrastructure

```bash
# Start PostgreSQL and pgAdmin (requires .env file copied from .env.example)
docker-compose up -d

# Access points:
# - PostgreSQL: localhost:5432
# - pgAdmin: localhost:5050
# - Server API: localhost:8080
# - GraphiQL: localhost:8080/graphiql
# - REST API: localhost:8080/api
```

## Architecture

### Server Domain Structure

Each Fitbit data type follows a consistent pattern under `server/src/main/kotlin/kenny/fitbitkotlin/{domain}/`:

- `{Domain}Model.kt` - JPA entities
- `{Domain}Repository.kt` - Spring Data JPA repository with custom queries
- `{Domain}Resolver.kt` - GraphQL `@Controller` with `@QueryMapping` methods
- `{Domain}Importer.kt` - Interface extending `Importer<T>` for JSON file imports
- `{Domain}ImporterJpa.kt` - Implementation that parses JSON and persists to database

Domains: heartrate, steps, calories, distance, exercise, sleep, profile

### Data Import System

The `Importer<T>` interface in `Importers.kt` provides:
- Concurrent file processing with configurable semaphore (`maxConcurrentFiles`)
- Batch inserts for performance (`batchSize`)
- Files read from `../data/{directory}` matching `filePattern()` regex
- Imported files renamed with `.imported` suffix

Import triggered via CLI args to `FitbitKotlinApplication` (see `ImportRunner`).

### GraphQL

Schema at `server/src/main/resources/graphql/schema.graphqls`. Uses `graphql-java-extended-scalars` for `DateTime` scalar. Resolvers use pagination with `PageRequest` and date range filtering via `DateRange` input type.

### Tech Stack

- Kotlin 2.3.0 / JVM 25 / Spring Boot 3.4.4
- PostgreSQL 17 with JPA/Hibernate
- GraphQL + REST (Spring Data REST at `/api`)
- React 18 + TypeScript + Apollo Client + Recharts
