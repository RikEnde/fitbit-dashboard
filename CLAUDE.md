# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build and Run Commands

### Server (Kotlin/Spring Boot)

```bash
# Build and run the server (API/GraphQL)
mvn -pl server spring-boot:run

# Run tests
mvn -pl server test

# Run a single test
mvn -pl server test -Dtest=StepsResolverTest

# Compile only
mvn -pl server compile
```

### Model (Shared Entities and Repositories)

```bash
# Compile only
mvn -pl model compile

# Install to local repository (required before building server/importer separately)
mvn -pl model install -DskipTests
```

### Importer (Data Import Module)

```bash
# Run data import (from ../data directory)
mvn -pl importer spring-boot:run -Dspring-boot.run.arguments="--heartrate --steps --calories"

# Import options: --heartrate, --steps, --calories, --distance, --exercise, --sleep,
# --sleepscore, --restingheartrate, --timeinzone, --activityminutes, --activezoneminutes,
# --vo2max, --runvo2max, --activitygoals, --devicetemperature, --respiratoryrate,
# --hrv, --hrvdetails, --minutespo2, --computedtemperature, --respiratoryratesummary, --dailyspo2,
# --profile

# Run tests
mvn -pl importer test

# Compile only
mvn -pl importer compile
```

### Dashboard (SvelteKit/TypeScript)

```bash
cd dashboard
npm run dev      # Dev server on port 3000, proxies to :8080
npm run build    # Production build
npm run preview  # Preview production build
```

### Infrastructure

```bash
docker-compose up -d

# Access points:
# - PostgreSQL: localhost:5432
# - pgAdmin: localhost:5050
# - Server API: localhost:8080 (requires auth)
# - GraphiQL: localhost:8080/graphiql (requires auth)
# - REST API: localhost:8080/api (requires auth)
```

## Architecture

### Module Structure

The project consists of four main modules:

- **model** - Shared JPA entities and Spring Data repositories (used by server and importer)
- **server** - REST API and GraphQL server with resolvers and exporters
- **importer** - Data import CLI for Fitbit JSON/CSV files
- **dashboard** - SvelteKit web dashboard for visualizing Fitbit data

Dependency graph:
```
        model
       /     \
      v       v
   server   importer
```

### Model Module Structure

Shared entities and repositories in `model/src/main/kotlin/kenny/fitbit/{domain}/`:

- `{Domain}Model.kt` - JPA entities
- `{Domain}Repository.kt` - Spring Data JPA repository with custom queries

Domains: heartrate, steps, calories, distance, exercise, sleep, profile

### Server Domain Structure

Server-specific code in `server/src/main/kotlin/kenny/fitbit/{domain}/`:

- `{Domain}Resolver.kt` - GraphQL `@Controller` with `@QueryMapping` methods
- `{Domain}Exporter.kt` - Interface extending `Exporter<T>` for Apple Health XML export

Heart rate domain has two resolvers:
- `HeartRateResolver.kt` - Real-time heart rate readings
- `RestingHeartRateResolver.kt` - Daily resting heart rate (single date or time series)

### Importer Structure

Import code is in `importer/src/main/kotlin/kenny/fitbit/`:

Core classes:
- `Importer.kt` - Contains `Importer<T>` interface, `JsonImporter<T>`, and `CsvImporter<T>` with built-in JPA batch persistence

Domain importers in `{domain}/`:
- `{Domain}Importer.kt` - Interface extending `Importer<T>`
- `{Domain}ImporterJpa.kt` - Implementation extending `JsonImporter` or `CsvImporter`

Features:
- Format-specific base classes (no JSON dependency for CSV importers)
- Built-in JPA batch persistence with flush/clear
- Concurrent file processing with configurable semaphore
- Configurable batch sizes per importer
- Files read from `../data/{directory}` matching `filePattern()` regex
- Imported files renamed with `.imported` suffix

Import triggered via CLI args to `FitbitImporterApplication` (see `ImportRunner`).

### Data Export System

The `Exporter<T>` interface in `Exporters.kt` provides Apple Health XML export:
- Generates XML compatible with iOS "Health Data Importer" app
- REST endpoints at `/api/export/{type}?from=...&to=...`
- Types: heartrate, steps, calories, distance, sleep
- Paginated queries to handle large datasets

```bash
# Export heart rate data for a year (requires auth)
curl -u $FITBIT_API_USER:$FITBIT_API_PASSWORD "http://localhost:8080/api/export/heartrate?from=2024-01-01T00:00:00&to=2024-12-31T23:59:59" -o heartrate.xml
```

### GraphQL

Schema at `server/src/main/resources/graphql/schema.graphqls`. Uses `graphql-java-extended-scalars` for `DateTime` and `Date` scalars. Resolvers use pagination with `PageRequest` and date range filtering via `DateRange` input type.

Key queries:
- `heartRates(limit, offset, range)` - Heart rate readings
- `restingHeartRate(date: Date!)` - Resting heart rate for a specific date
- `restingHeartRates(limit, offset, range)` - Resting heart rate time series
- `steps`, `calories`, `distances`, `exercises`, `sleeps`, `sleepScores` - Other health data

### Security & Authentication

All API endpoints (`/graphql`, `/graphiql`, `/api/**`) require HTTP Basic Authentication via Spring Security. See `security.md` for full details.

Key files:
- `server/src/main/kotlin/kenny/fitbit/SecurityConfig.kt` - SecurityFilterChain, user config
- `dashboard/src/lib/stores/auth.ts` - Credentials store (sessionStorage)
- `dashboard/src/lib/components/Login.svelte` - Login form

Credentials are configured via environment variables (`FITBIT_API_USER`, `FITBIT_API_PASSWORD`) in `.envrc` (gitignored). No defaults - server fails to start without them.

```bash
# Authenticated curl
curl -u $FITBIT_API_USER:$FITBIT_API_PASSWORD http://localhost:8080/api

# Export with auth
curl -u $FITBIT_API_USER:$FITBIT_API_PASSWORD "http://localhost:8080/api/export/heartrate?from=2024-01-01T00:00:00&to=2024-12-31T23:59:59" -o heartrate.xml
```

### Dashboard Structure

The SvelteKit dashboard in `dashboard/src/lib/`:

- `components/layout/` - Header, ProfileAvatar, ProfileDropdown
- `components/tiles/` - StepsTile, CaloriesTile, DistanceTile, HeartRateTile, SleepTile, ActiveMinutesTile
- `components/charts/` - ProgressRing, MiniBarChart, BarChart, LineChart, SleepStagesChart
- `components/Login.svelte` - Login form (validates credentials against server)
- `graphql/client.ts` - URQL GraphQL client (sends Basic Auth header)
- `stores/` - Svelte stores for dashboard state, preferences, profile, auth
- `utils/` - Colors, formatters, date utilities

Detail pages in `dashboard/src/routes/`:
- `/steps` - 30-day trend, hourly breakdown, weekly averages
- `/heartrate` - Resting HR, min/max stats, day chart, zones distribution, resting HR trend (30-day/1-year toggle)
- `/sleep` - Stages timeline, score breakdown, 30-day trend
- `/exercise` - Activity list with HR zones, 30-day trend
- `/calories` - 30-day trend, hourly breakdown
- `/distance` - 30-day trend, hourly breakdown (values in km)
- `/profile` - User info, physical stats, stride lengths, unit preferences

Key patterns:
- Uses Svelte 5 runes (`$state`, `$derived`, `$props`, `$effect`)
- GraphQL queries via URQL with client-side fetching in `onMount`
- Reactive date range filtering via stores
- TailwindCSS with custom Fitbit color palette

Data unit notes:
- Distance values from Fitbit are stored in centimeters (divide by 100,000 for km)
- Stride length values are stored in centimeters (divide by 2.54 for inches)
- Exercise duration is in milliseconds (divide by 60,000 for minutes)
- Resting heart rate is stored as a float (bpm) with an error margin field

### Tech Stack

- Kotlin 2.3.0 / JVM 25 / Spring Boot 3.4.4
- PostgreSQL 17 with JPA/Hibernate
- GraphQL + REST (Spring Data REST at `/api`)
- SvelteKit 2 + Svelte 5 + TypeScript + URQL + TailwindCSS
