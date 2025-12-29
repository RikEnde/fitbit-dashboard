# Fitbit Kotlin Server - Architecture & Tech Stack

## Overview

A Spring Boot 3.4 Kotlin application for importing, storing, and querying Fitbit wearable health data through GraphQL and REST APIs. The application provides comprehensive analytics for 20+ health metrics including steps, heart rate, sleep, exercise, calories, distance, and respiratory data.

## Tech Stack

### Core Technologies
- **Language:** Kotlin 2.3.0
- **Runtime:** Java 25 JVM
- **Framework:** Spring Boot 3.4.4
- **Database:** PostgreSQL 17
- **Build Tool:** Maven

### Key Dependencies
- **Spring Boot Data JPA** - ORM and database access
- **Spring Boot Web** - REST/MVC support
- **Spring Data REST** - Auto-generated REST endpoints
- **Spring GraphQL** - GraphQL API implementation
- **Kotlinx Coroutines 1.10.1** - Asynchronous file processing
- **Jackson Kotlin Module** - JSON serialization/deserialization
- **GraphQL Java Extended Scalars 22.0** - DateTime support
- **Jakarta Persistence API** - JPA entity annotations

### Development & Testing
- **JUnit 5** - Testing framework
- **Spring Boot Test** - Integration testing
- **Spring GraphQL Test** - GraphQL query testing
- **Docker Compose** - Containerized database deployment

## Architecture

### Architectural Pattern
The application follows a **layered domain-driven architecture** with clear separation of concerns:

```
┌─────────────────────────────────────┐
│         API Layer                   │
│  GraphQL Resolvers + REST Endpoints │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│       Service Layer                 │
│  Importer Components (Async)        │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│     Data Access Layer               │
│  JPA Repositories + Specifications  │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│       Domain Layer                  │
│  JPA Entity Models                  │
└─────────────────────────────────────┘
```

### Design Patterns

1. **Repository Pattern** - JPA repositories for data access abstraction
2. **Importer Pattern** - Format-specific abstract classes (`JsonImporter<T>`, `CsvImporter<T>`) for async file processing with built-in JPA batch persistence
3. **Resolver Pattern** - GraphQL controllers with `@QueryMapping` and `@SchemaMapping`
4. **Specification Pattern** - `JpaSpecificationExecutor` for dynamic queries
5. **Application Runner Pattern** - CLI-based import trigger on startup

## Project Structure

```
server/src/main/kotlin/kenny/fitbit/
├── FitbitKotlinApplication.kt      # Entry point
├── GraphQLConfig.kt                # GraphQL scalar configuration
├── GraphiQlConfiguration.kt        # Custom GraphiQL controller
├── Exporters.kt                    # Exporter<T> interface + ExportController + AppleHealthXmlWriter
├── calories/                       # Calories module
│   ├── CaloriesModel.kt            # JPA entity
│   ├── CaloriesRepository.kt       # Data access
│   ├── CaloriesResolver.kt         # GraphQL queries
│   └── CaloriesExporter.kt         # Apple Health XML export
├── distance/                       # Distance module
├── exercise/                       # Exercise & activity module
├── heartrate/                      # Heart rate & HRV module
├── profile/                        # User profile module
├── steps/                          # Steps module
└── sleep/                          # Sleep & respiratory module

importer/src/main/kotlin/kenny/fitbit/importer/
├── FitbitImporterApplication.kt    # Importer entry point + ImportRunner
├── Importer.kt                     # Importer<T> interface + JsonImporter + CsvImporter
├── calories/                       # Calories importer
│   ├── CaloriesImporter.kt         # Import interface
│   └── CaloriesImporterJpa.kt      # Import implementation (extends JsonImporter)
├── distance/                       # Distance importer
├── exercise/                       # Exercise & activity importers
├── heartrate/                      # Heart rate & HRV importers
├── profile/                        # Profile importer
├── steps/                          # Steps importer
└── sleep/                          # Sleep & respiratory importers

server/src/main/resources/
├── application.yml                 # Spring configuration
├── graphql/schema.graphqls         # GraphQL schema
└── graphiql/index.html             # GraphiQL IDE (served via GraphiQlController)

docker-compose.yml                  # PostgreSQL + pgAdmin
```

**Total:** ~45 Kotlin source files across server and importer modules

## Core Components

### 1. Domain Modules

Each health metric is organized as a self-contained module:

| Module | Purpose | Key Entities |
|--------|---------|--------------|
| **Steps** | Activity step tracking | Steps |
| **Heart Rate** | Cardiac metrics | HeartRate, RestingHeartRate, DailyHeartRateVariability, HeartRateVariabilityDetails, TimeInHeartRateZones |
| **Calories** | Energy expenditure | Calories |
| **Distance** | Movement distance | Distance |
| **Exercise** | Workout sessions | Exercise, HeartRateZone, ActivityLevel, ActivityMinutes, DemographicVO2Max, RunVO2Max, ActivityGoal |
| **Sleep** | Sleep analysis | Sleep, SleepLevelSummary, SleepLevelData, SleepLevelShortData, SleepScore, DeviceTemperature, DailyRespiratoryRate, MinuteSpO2, ComputedTemperature, RespiratoryRateSummary, DailySpO2 |
| **Profile** | User demographics | Profile (30+ attributes) |

### 2. Importer Architecture

**Purpose:** Async file processing engine for Fitbit JSON/CSV data

**Architecture:**
```
Importer<T> (interface)
├── directory(): String
├── filePattern(): String
├── files(): List<File>
└── import(): Int

JsonImporter<T> (abstract class)
├── repository: JpaRepository<T, *>
├── entityManager: EntityManager
├── parseToEntity(JsonNode): T?
├── batchSize, maxConcurrentFiles
├── saveBatch() - @Transactional batch persistence
└── Handles JSON parsing + batching + persistence

CsvImporter<T> (abstract class)
├── repository: JpaRepository<T, *>
├── entityManager: EntityManager
├── parseRow(values, headers): T?
├── batchSize, maxConcurrentFiles
├── saveBatch() - @Transactional batch persistence
└── Handles CSV parsing + batching + persistence
```

**Key Features:**
- Format-specific base classes (no JSON dependency for CSV importers)
- Built-in JPA batch persistence with flush/clear
- Kotlin coroutines with `Dispatchers.IO` for parallel imports
- CLI options for selective imports (--heartrate, --steps, --exercise, etc.)
- File pattern matching with regex
- Automatic `.imported` suffix marking
- Configurable batch sizes and concurrency

**Supported Import Types:**
- Heart rate, Steps, Exercise, Calories, Distance
- Sleep, Sleep scores, Respiratory rates
- Temperature, SpO2, VO2 Max
- Activity goals, Activity levels
- And more (20+ types total)

### 3. GraphQL API

**Endpoint:** `/graphql` (with GraphiQL at `/graphiql`)

**Key Queries:**
```graphql
# Health check
healthStatus

# Time-series data with pagination
heartRates(limit: Int!, offset: Int!, range: DateRange)
steps(limit: Int!, offset: Int!, range: DateRange)
exercises(limit: Int!, offset: Int!, range: DateRange)
sleeps(limit: Int!, offset: Int!, range: DateRange)
calories(limit: Int!, offset: Int!, range: DateRange)
distances(limit: Int!, offset: Int!, range: DateRange)

# Aggregations
dailyStepsSum(range: DateRange!)
weeklyStepsAverage(range: DateRange!)
heartRatesPerInterval(range: DateRange!)

# Profile data
profiles(limit: Int!, offset: Int!)
profile(id: ID!)
```

**Features:**
- Custom DateTime scalar (RFC-3339 format)
- DateRange input type for filtering
- Nested entity resolution
- Pagination support

### 4. REST API

**Endpoint:** `/api` (Spring Data REST)

Auto-generated CRUD endpoints for all repositories with:
- Pagination
- Sorting
- Filtering
- HATEOAS links

### 5. Apple Health Export API

**Endpoint:** `/api/export/{type}`

Exports Fitbit data to Apple Health XML format for import via iOS apps like "Health Data Importer".

**Supported Types:**
- `/api/export/heartrate` → HKQuantityTypeIdentifierHeartRate
- `/api/export/steps` → HKQuantityTypeIdentifierStepCount
- `/api/export/calories` → HKQuantityTypeIdentifierActiveEnergyBurned
- `/api/export/distance` → HKQuantityTypeIdentifierDistanceWalkingRunning
- `/api/export/sleep` → HKCategoryTypeIdentifierSleepAnalysis

**Parameters:**
- `from` - Start datetime (ISO format: `2024-01-01T00:00:00`)
- `to` - End datetime (ISO format: `2024-12-31T23:59:59`)

**Example:**
```bash
curl "http://localhost:8080/api/export/heartrate?from=2024-01-01T00:00:00&to=2024-12-31T23:59:59" -o heartrate.xml
```

### 6. Data Access Layer

**Repositories:** JPA repositories extending `JpaRepository` and `JpaSpecificationExecutor`

**Custom Queries:**
- Native SQL for complex aggregations
- Window functions for weekly averages
- Time bucketing (10-minute intervals)
- Date-range filtering
- Daily summations with GROUP BY

**Example Advanced Query:**
```kotlin
@Query(nativeQuery = true)
fun findHeartRatesPerInterval(startDate: LocalDateTime, endDate: LocalDateTime): List<Any>
// Uses generate_series() for 10-minute buckets
```

## Database Architecture

### Configuration
- **DBMS:** PostgreSQL 17-Alpine (Docker)
- **Connection:** jdbc:postgresql://localhost:5432/fitbit_db
- **ORM:** Hibernate with JPA
- **DDL Mode:** update (auto schema migration)

### Performance Optimizations

1. **Database Indexes:**
   - Time/date columns for time-series queries
   - Unique constraints on logical keys (e.g., sleep log IDs)
   - Composite indexes for common query patterns

2. **Fetch Strategies:**
   - LAZY fetching for nested collections (default)
   - Fetch joins or DTO projections when related data is needed
   - Avoids N+1 queries and OOM on large datasets

3. **Entity Relationships:**
   - One-to-many with cascade delete and orphan removal
   - Proper bidirectional mappings

### Example Index Definition
```kotlin
@Entity
@Table(
    indexes = [
        Index(name = "idx_steps_date_time", columnList = "date_time")
    ]
)
class Steps { ... }
```

## Data Pipeline

### Import Flow
```
1. Application Startup
   ↓
2. ImportRunner Triggered (with CLI options)
   ↓
3. File Pattern Matching (regex via Importer.files())
   ↓
4. Parallel Async Processing (Kotlin Coroutines + Semaphore)
   ↓
5. Format-Specific Parsing
   ├── JsonImporter: Jackson ObjectMapper
   └── CsvImporter: BufferedReader
   ↓
6. Entity Mapping (parseToEntity/parseRow)
   ↓
7. Batched Persistence (saveBatch with flush/clear)
   ↓
8. File Renaming (.imported suffix)
```

### Supported File Formats
- **JSON:** Heart rate, steps, exercise, calories, distance, sleep, etc.
- **CSV:** Activity goals, sleep scores, respiratory rates

## Configuration

### Application Configuration (`application.yml`)

```yaml
spring:
  graphql:
    graphiql:
      enabled: true
      path: /graphiql
    path: /graphql
    schema:
      printer:
        enabled: true
  datasource:
    url: jdbc:postgresql://localhost:5432/${POSTGRES_DB}
    username: ${POSTGRES_USER}
    password: ${POSTGRES_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: update
    properties:
      hibernate:
        format_sql: true
        jdbc:
          batch_size: 1000
        order_inserts: true
        order_updates: true
  data:
    rest:
      base-path: /api
server:
  port: 8080
```

### Docker Compose Services
- **PostgreSQL 17** - Primary database
- **pgAdmin** - Database administration UI

## API Examples

### GraphQL Query Example
```graphql
query {
  dailyStepsSum(range: {
    from: "2024-01-01T00:00:00Z"
    to: "2024-01-31T23:59:59Z"
  }) {
    date
    totalSteps
  }
}
```

### REST API Example
```
GET /api/steps?page=0&size=20&sort=dateTime,desc
```

## Special Features

### 1. Advanced Time-Series Analytics
- Heart rate bucketing into 10-minute intervals
- Daily aggregations (steps, calories)
- Weekly averages with moving windows
- Custom date range filtering

### 2. Complex Entity Relationships
- Sleep data with 3 levels of nested details
- Exercise with heart rate zones and activity breakdowns
- Historical temperature and respiratory tracking

### 3. Asynchronous Processing
- Parallel file imports using coroutines with configurable semaphore (`maxConcurrentFiles`)
- Batch inserts with configurable size (default 5000, customizable per importer)
- Format-specific base classes (`JsonImporter`, `CsvImporter`) handle concurrency and persistence
- Non-blocking I/O operations
- Progress tracking and logging

### 4. Multi-Format Support
- JSON parsing for raw Fitbit exports
- CSV parsing for aggregate data
- Flexible schema mapping

## Security Considerations

**Current State:**
- No authentication/authorization on API endpoints
- Database credentials in application.yml (consider using environment variables for production)
- Docker Compose uses `.env` file for PostgreSQL credentials

**Recommended Enhancements:**
- Spring Security integration
- JWT-based authentication
- Environment variable configuration for sensitive data
- API rate limiting

## Deployment

### Local Development
```bash
# Start database
docker-compose up -d

# Run server (API/GraphQL)
mvn -pl server spring-boot:run

# Run importer with options
mvn -pl importer spring-boot:run -Dspring-boot.run.arguments="--heartrate --steps --sleep"
```

### Accessing Services
- **GraphQL API:** http://localhost:8080/graphql
- **GraphiQL IDE:** http://localhost:8080/graphiql
- **REST API:** http://localhost:8080/api
- **pgAdmin:** http://localhost:5050

## Testing

### Test Coverage
- Unit tests for importers and exporters
- Spring Boot integration tests
- GraphQL query tests
- Repository tests

### Running Tests
```bash
# Server tests
mvn -pl server test

# Importer tests
mvn -pl importer test

# Run a single test
mvn -pl importer test -Dtest=AccountImporterImplTest
```

## Future Enhancements

1. **Security:** Implement authentication and authorization
2. **Real-time:** WebSocket support for live data streaming
3. **Analytics:** Machine learning insights from health data
4. **Export:** CSV and PDF report exports (Apple Health XML export implemented)
5. **Notifications:** Alerts for health metric anomalies
6. **Mobile:** REST API optimization for mobile clients

## Dashboard Module

The `dashboard/` directory contains a SvelteKit web dashboard for visualizing Fitbit data:

- **Stack:** SvelteKit 2, Svelte 5, TypeScript, URQL, TailwindCSS
- **Dev server:** `npm run dev` (port 3000, proxies API calls to :8080)
- **Features:**
  - Tile-based dashboard with Steps, Calories, Distance, Heart Rate, Sleep, Active Minutes
  - Detail pages for each metric with 30-day trends and hourly breakdowns
  - Profile page with user info and settings
  - Interactive charts with date selection
  - Svelte 5 runes mode (`$state`, `$derived`, `$props`, `$effect`)

See `dashboard/agents.md` for detailed Svelte development patterns.

## Build Note

**Important:** The importer module depends on the server module. Before building the importer, install the server module first:

```bash
mvn -pl server install -DskipTests
mvn -pl importer compile
```

Or build from the parent to ensure correct ordering:
```bash
mvn compile
```

## Summary

This is a production-ready, data-centric health analytics platform that combines:
- Modern Spring Boot 3 + Kotlin stack
- Dual GraphQL/REST API architecture
- Sophisticated async file importing with format-specific parsers and built-in batch persistence
- Optimized PostgreSQL storage
- Advanced time-series analytics
- Modular domain-driven design with separate server and importer modules

The application is well-suited for storing and analyzing large volumes of Fitbit wearable data with performance optimizations and comprehensive query capabilities.
