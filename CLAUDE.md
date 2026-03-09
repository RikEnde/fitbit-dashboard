# CLAUDE.md

## Architecture

Maven multi-module: `model` → `importer` → `importer-cli` / `server`. Dashboard is SvelteKit (npm).

DDD by domain package `kenny.fitbit.{domain}` across all modules. Each domain has:
- `model/` — Entity + Repository
- `importer/` — Interface (directory + filePattern) + Impl extending `JsonImporter<T>` or `CsvImporter<T>`
- `server/` — GraphQL `@Controller` resolver, optional `Exporter<T>`
- `dashboard/` — GQL query in `queries.ts`, route page `routes/{domain}/+page.svelte`, tile in `components/tiles/`

Use an existing domain as reference: `steps` for simple, `heartrate` for complex with aggregation, `sleep` for aggregate roots with child entities.

Wiring a new importer: add CLI flag in `ImportRunner.kt`, register in `ImportService.kt` for REST import, add subdirectory to `ZipImportService.kt` known directories.

## Critical Constraints

### GraphQL Scalar Types
All JPA entity timestamp fields use `LocalDateTime`, which **cannot** be serialized by `ExtendedScalars.DateTime` (it requires `OffsetDateTime`). These fields must remain typed as `String!` in the schema — Hibernate serializes `LocalDateTime` via `.toString()` producing ISO-8601 format. Only fields that explicitly return `OffsetDateTime` in the resolver (e.g. `SumsOfHeartRates.timeInterval`) can use `DateTime!`. The `Date!` scalar works correctly with `LocalDate`.

### Database Policy
This is a development project. Do not create database migration scripts. Schema changes are handled by dropping and recreating the database.

### Data Integrity
When import or runtime behavior doesn't match expectations, consider that the test data or real data may be incorrect before changing code. Ask the user to verify the data first.

### Profile Scoping
All health data entities have a `profile` FK. Every repository query must filter by profile. Never return unscoped data.

## Build Commands

```bash
mvn -pl model install -DskipTests        # required before building server or importer
mvn -pl importer install -DskipTests     # required before building server or importer-cli
mvn -pl server spring-boot:run           # run server on :8080
mvn -pl server test                      # run server tests
mvn -pl server test -Dtest=SomeTest      # run a single test
mvn -pl importer test                    # run importer tests
cd dashboard && npm run dev              # dashboard dev server on :3000, proxies to :8080
```

## Importer CLI

```bash
mvn -pl importer-cli spring-boot:run -Dspring-boot.run.arguments="--heartrate --steps --all --user=NAME --datadir=PATH"
```

Flags: `--heartrate`, `--steps`, `--calories`, `--distance`, `--exercise`, `--sleep`, `--sleepscore`, `--restingheartrate`, `--timeinzone`, `--activityminutes`, `--activezoneminutes`, `--vo2max`, `--runvo2max`, `--activitygoals`, `--devicetemperature`, `--respiratoryrate`, `--hrv`, `--hrvdetails`, `--minutespo2`, `--computedtemperature`, `--respiratoryratesummary`, `--dailyspo2`, `--all`

Profile is always imported first, unconditionally. `--datadir` defaults to `../data`.

## Importer Behavior

- Processed files are renamed with an `.imported` suffix.
- Data path: `{dataDir}/{username}/{subdirectory}/` (e.g. `../data/YourName/Physical Activity/heart_rate-2024-01-01.json`).
- **CLI**: directory name becomes the profile `username`.
- **REST/UI** (`POST /api/import`): accepts a Fitbit export zip; auto-detects the user folder by looking for known subdirectory names (`Physical Activity`, `Sleep`, `Heart Rate Variability`, etc.). Supports three layouts — data at zip root, user folder at zip root, or user folder inside a wrapper folder. The profile `username` is always overridden with the authenticated user's login name after import, regardless of the folder name in the zip.
