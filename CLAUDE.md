# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Working with data

When import or runtime behavior doesn't match expectations, consider that the test data or real data may be incorrect before changing code. Ask the user to verify the data first.

## Build and Run Commands

### Server (Kotlin/Spring Boot)

```bash
mvn -pl server spring-boot:run          # Run server (API/GraphQL) on :8080
mvn -pl server test                      # Run all server tests
mvn -pl server test -Dtest=StepsResolverTest  # Run a single test
mvn -pl server compile                   # Compile only
```

### Model (Shared Entities and Repositories)

```bash
mvn -pl model compile                    # Compile only
mvn -pl model install -DskipTests        # Install to local repo (required before building server/importer separately)
```

### Importer (Data Import CLI)

```bash
# Import specific stat types for all users
mvn -pl importer spring-boot:run -Dspring-boot.run.arguments="--heartrate --steps --calories"

# Import all stat types for a specific user
mvn -pl importer spring-boot:run -Dspring-boot.run.arguments="--all --user=RikEnde"

# Available flags: --heartrate, --steps, --calories, --distance, --exercise, --sleep,
# --sleepscore, --restingheartrate, --timeinzone, --activityminutes, --activezoneminutes,
# --vo2max, --runvo2max, --activitygoals, --devicetemperature, --respiratoryrate,
# --hrv, --hrvdetails, --minutespo2, --computedtemperature, --respiratoryratesummary,
# --dailyspo2, --profile, --all

mvn -pl importer test                    # Run all importer tests
mvn -pl importer compile                 # Compile only
```

### Dashboard (SvelteKit/TypeScript)

```bash
cd dashboard
npm run dev      # Dev server on port 3000, proxies /graphql and /api to :8080
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

### Multi-Tenancy

All data is scoped to a user profile. The system supports multiple users with data isolation:

- **Authentication**: Database-backed HTTP Basic Auth via `SecurityConfig.kt`. Credentials stored in `user_credentials` table (BCrypt hashed). No defaults — requires DB-seeded credentials.
- **Profile scoping**: `AuthenticatedProfileService` extracts the authenticated username from `SecurityContextHolder` and loads the corresponding `Profile`. All resolvers and exporters inject this service and pass the profile to repository queries.
- **Data isolation**: Every health data entity (Steps, HeartRate, Sleep, etc.) has a `profile` FK. Repository methods filter by profile.
- **Import log**: After each import, `ImportLog` records the latest data date per profile and stat type. The `latestDataDate` GraphQL query returns the most recent date with data for the authenticated user, which the dashboard uses as its default date.

### Naming Conventions

**Packages**: `kenny.fitbit.{domain}` — same package name across all three Kotlin modules (model, server, importer). Domains: `calories`, `distance`, `exercise`, `heartrate`, `sleep`, `steps`, `profile`, `auth`, `importlog`.

**Model module** (`model/src/main/kotlin/kenny/fitbit/{domain}/`):
- `{Domain}Model.kt` — JPA entity classes (e.g., `HeartRate`, `RestingHeartRate`)
- `{Domain}Repository.kt` — Spring Data JPA repository interfaces

**Server module** (`server/src/main/kotlin/kenny/fitbit/{domain}/`):
- `{Domain}Resolver.kt` — GraphQL `@Controller` with `@QueryMapping` methods
- `{Domain}Exporter.kt` — `Exporter<T>` implementation for Apple Health XML export

Heart rate domain has two resolvers: `HeartRateResolver.kt` (real-time readings) and `RestingHeartRateResolver.kt` (daily resting HR).

**Importer module** (`importer/src/main/kotlin/kenny/fitbit/{domain}/`):
- `{Domain}Importer.kt` — interface extending `Importer<T>`, defines `directory()` and `filePattern()`
- `{Domain}ImporterJpa.kt` — contains `{Domain}ImporterImpl` class extending `JsonImporter` or `CsvImporter`

**GraphQL query methods**: plural nouns matching the schema — `fun steps(...)`, `fun heartRates(...)`, `fun exercises(...)`. Aggregation queries use descriptive names: `dailyStepsSum`, `weeklyStepsAverage`, `heartRatesPerInterval`.

**Tests**:
- Server: `{Domain}ExporterImplTest.kt`, `SecurityConfigTest.kt`
- Importer: `{Domain}ImporterImplTest.kt`

### Importer Structure

Core classes in `importer/src/main/kotlin/kenny/fitbit/Importer.kt`:
- `Importer<T>` interface — defines `directory()`, `filePattern()`, `files()`, `import()`
- `JsonImporter<T>` — abstract base for JSON files with `parseToEntity(JsonNode): T?`
- `CsvImporter<T>` — abstract base for CSV files with `parseRow(values, headers): T?`

Both base classes handle concurrent file processing (coroutines + semaphore), batched JPA persistence (flush/clear), and automatic `.imported` suffix renaming.

Data lives in `../data/{username}/{subdirectory}/` (e.g., `../data/RikEnde/Physical Activity/heart_rate-2024-01-01.json`). The directory name becomes the username. `ImportRunner` orchestrates: scans user directories, imports profile first, then each enabled stat type.

### Data Export System

The `Exporter<T>` interface in `Exporters.kt` provides Apple Health XML export:
- Generates XML compatible with iOS "Health Data Importer" app
- REST endpoints at `/api/export/{type}?from=...&to=...`
- Types: heartrate, steps, calories, distance, sleep
- Paginated queries to handle large datasets
- Dashboard UI: Profile dropdown → "Export to Apple Health" opens an export dialog (select data type + date range)
- Only Apple Health XML format is currently supported

```bash
# Export heart rate data for a year (requires auth)
curl -u user:password "http://localhost:8080/api/export/heartrate?from=2024-01-01T00:00:00&to=2024-12-31T23:59:59" -o heartrate.xml
```

### GraphQL

Schema at `server/src/main/resources/graphql/schema.graphqls`. Uses `graphql-java-extended-scalars` for `DateTime` and `Date` scalars. Resolvers use pagination with `PageRequest` and date range filtering via `DateRange` input type.

Key queries:
- `profile` - Authenticated user's profile
- `latestDataDate` - Most recent date with imported data for the authenticated user
- `heartRates(limit, offset, range)` - Heart rate readings
- `restingHeartRate(date: Date!)` - Resting heart rate for a specific date
- `restingHeartRates(limit, offset, range)` - Resting heart rate time series
- `steps`, `calories`, `distances`, `exercises`, `sleeps`, `sleepScores` - Other health data
- `dailyStepsSum`, `weeklyStepsAverage`, `heartRatesPerInterval` - Aggregations

### Security & Authentication

All API endpoints (`/graphql`, `/graphiql`, `/api/**`) require HTTP Basic Authentication via Spring Security (stateless, CSRF disabled).

Key files:
- `server/src/main/kotlin/kenny/fitbit/SecurityConfig.kt` - SecurityFilterChain, database-backed UserDetailsService
- `server/src/main/kotlin/kenny/fitbit/AuthenticatedProfileService.kt` - Loads authenticated user's Profile from SecurityContext
- `dashboard/src/lib/stores/auth.ts` - Credentials store (sessionStorage), login/logout functions
- `dashboard/src/lib/components/Login.svelte` - Login form (validates credentials against server)
- `dashboard/src/lib/graphql/client.ts` - URQL client (sends Basic Auth header on all requests)

```bash
# Authenticated API calls
curl -u user:password http://localhost:8080/api
curl -u user:password "http://localhost:8080/api/export/heartrate?from=2024-01-01T00:00:00&to=2024-12-31T23:59:59" -o heartrate.xml
```

### Dashboard

SvelteKit 2 + Svelte 5 + TypeScript + URQL + TailwindCSS.

Key patterns:
- Uses Svelte 5 runes (`$state`, `$derived`, `$props`, `$effect`)
- GraphQL queries via URQL with client-side fetching in `onMount`
- On login: fetches `profile` and `latestDataDate`, sets dashboard to most recent data date
- Reactive date range filtering via stores
- TailwindCSS with custom Fitbit color palette

Detail pages in `dashboard/src/routes/`:
- `/steps` - 30-day trend, hourly breakdown, weekly averages
- `/heartrate` - Resting HR, min/max stats, day chart, zones distribution, resting HR trend (30-day/1-year toggle)
- `/sleep` - Stages timeline, score breakdown, 30-day trend
- `/exercise` - Activity list with HR zones, 30-day trend
- `/calories` - 30-day trend, hourly breakdown
- `/distance` - 30-day trend, hourly breakdown (values in km)
- `/profile` - User info, physical stats, stride lengths, unit preferences

### Data Unit Notes

- Distance values from Fitbit are stored in centimeters (divide by 100,000 for km)
- Stride length values are stored in centimeters (divide by 2.54 for inches)
- Exercise duration is in milliseconds (divide by 60,000 for minutes)
- Resting heart rate is stored as a float (bpm) with an error margin field

### Tech Stack

- Kotlin 2.3.0 / JVM 25 / Spring Boot 3.4.4
- PostgreSQL 17 with JPA/Hibernate
- GraphQL + REST (Spring Data REST at `/api`)
- SvelteKit 2 + Svelte 5 + TypeScript + URQL + TailwindCSS
