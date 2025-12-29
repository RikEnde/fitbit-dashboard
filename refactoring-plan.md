# Refactoring Plan: Extract Model Module

## Current State

The project has two main modules with a problematic dependency:

```
server (models, repositories, resolvers, exporters, GraphQL/REST config)
   ^
   |
importer (import logic) ─── depends on server
```

The importer only needs models and repositories from server, but currently pulls in the entire server module including web controllers, GraphQL resolvers, and exporters.

### What Importer Actually Uses from Server

From analyzing imports in `importer/src/main/kotlin`:

**Models (JPA Entities):**
- `kenny.fitbit.steps.Steps`
- `kenny.fitbit.calories.Calories`
- `kenny.fitbit.distance.Distance`
- `kenny.fitbit.heartrate.*` (HeartRate, RestingHeartRate, TimeInHeartRateZones, TimeInHeartRateZoneValue, DailyHeartRateVariability, HeartRateVariabilityDetails)
- `kenny.fitbit.exercise.*` (Exercise, HeartRateZone, ActivityLevel, ActivityMinutes, DemographicVO2Max, RunVO2Max, ActivityGoal)
- `kenny.fitbit.sleep.*` (Sleep, SleepLevelSummary, SleepLevelData, SleepLevelShortData, SleepSmodel, DeviceTemperature, DailyRespiratoryRate, MinuteSpO2, ComputedTemperature, RespiratoryRateSummary, DailySpO2)
- `kenny.fitbit.profile.Profile`

**Repositories:**
- All `*Repository` interfaces corresponding to the models above

### What Server Contains That Importer Does NOT Need

- `*Resolver.kt` - GraphQL controllers (depend on Spring GraphQL)
- `*Exporter.kt` - Apple Health XML export (depend on Spring Web)
- `Exporters.kt` - REST controller and export utilities
- `GraphQLConfig.kt` - GraphQL scalar configuration (contains `DateRange` class used by resolvers)
- `GraphiQlConfiguration.kt` - GraphiQL IDE configuration
- `FitbitKotlinApplication.kt` - Main Spring Boot application

## Proposed Solution

Create a new `model` module containing shared models and repositories:

```
model (models, repositories)
   ^            ^
   |            |
server      importer
(resolvers, exporters, GraphQL/REST)    (import logic)
```

### New Module Structure

```
Fitbit-kotlin/
├── pom.xml (parent)
├── model/
│   ├── pom.xml
│   └── src/main/kotlin/kenny/fitbit/
│       ├── steps/
│       │   ├── StepsModel.kt
│       │   └── StepsRepository.kt
│       ├── calories/
│       │   ├── CaloriesModel.kt
│       │   └── CaloriesRepository.kt
│       ├── distance/
│       │   ├── DistanceModel.kt
│       │   └── DistanceRepository.kt
│       ├── heartrate/
│       │   ├── HeartRateModel.kt
│       │   └── HeartRateRepository.kt
│       ├── exercise/
│       │   ├── ExerciseModel.kt
│       │   └── ExerciseRepository.kt
│       ├── sleep/
│       │   ├── SleepModel.kt
│       │   └── SleepRepository.kt
│       └── profile/
│           ├── ProfileModel.kt
│           └── ProfileRepository.kt
├── server/
│   ├── pom.xml (depends on model)
│   └── src/main/kotlin/kenny/fitbit/
│       ├── FitbitKotlinApplication.kt
│       ├── GraphQLConfig.kt (keep DateRange here, only used by resolvers)
│       ├── GraphiQlConfiguration.kt
│       ├── Exporters.kt
│       ├── steps/
│       │   ├── StepsResolver.kt
│       │   └── StepsExporter.kt
│       ├── ... (other resolvers/exporters)
├── importer/
│   ├── pom.xml (depends on model, NOT server)
│   └── src/main/kotlin/kenny/fitbit/importer/
│       └── ... (unchanged)
└── dashboard/
    └── ... (unchanged)
```

## Implementation Steps

### Step 1: Create model module directory structure

```bash
mkdir -p model/src/main/kotlin/kenny/fitbit/{steps,calories,distance,heartrate,exercise,sleep,profile}
mkdir -p model/src/test/kotlin
```

### Step 2: Create model/pom.xml

Dependencies needed:
- `spring-boot-starter-data-jpa` (for JPA annotations and Spring Data repositories)
- `kotlin-stdlib`
- `kotlin-reflect`
- PostgreSQL driver (runtime scope)

Kotlin compiler plugins needed:
- `spring` (for @Repository)
- `jpa` (for @Entity no-arg constructor)
- `all-open` (for JPA entity classes)

### Step 3: Move model files from server to model

Files to move (keeping same package structure):
- `server/src/main/kotlin/kenny/fitbit/steps/StepsModel.kt` → `model/...`
- `server/src/main/kotlin/kenny/fitbit/calories/CaloriesModel.kt` → `model/...`
- `server/src/main/kotlin/kenny/fitbit/distance/DistanceModel.kt` → `model/...`
- `server/src/main/kotlin/kenny/fitbit/heartrate/HeartRateModel.kt` → `model/...`
- `server/src/main/kotlin/kenny/fitbit/exercise/ExerciseModel.kt` → `model/...`
- `server/src/main/kotlin/kenny/fitbit/sleep/SleepModel.kt` → `model/...`
- `server/src/main/kotlin/kenny/fitbit/profile/ProfileModel.kt` → `model/...`

### Step 4: Move repository files from server to model

Files to move:
- `server/src/main/kotlin/kenny/fitbit/steps/StepsRepository.kt` → `model/...`
- `server/src/main/kotlin/kenny/fitbit/calories/CaloriesRepository.kt` → `model/...`
- `server/src/main/kotlin/kenny/fitbit/distance/DistanceRepository.kt` → `model/...`
- `server/src/main/kotlin/kenny/fitbit/heartrate/HeartRateRepository.kt` → `model/...`
- `server/src/main/kotlin/kenny/fitbit/exercise/ExerciseRepository.kt` → `model/...`
- `server/src/main/kotlin/kenny/fitbit/sleep/SleepRepository.kt` → `model/...`
- `server/src/main/kotlin/kenny/fitbit/profile/ProfileRepository.kt` → `model/...`

### Step 5: Update parent pom.xml

Add `model` module to the modules list (before server):
```xml
<modules>
    <module>model</module>
    <module>server</module>
    <module>importer</module>
</modules>
```

### Step 6: Update server/pom.xml

Replace any internal model/repository references with dependency on model:
```xml
<dependency>
    <groupId>kenny</groupId>
    <artifactId>fitbit-model</artifactId>
    <version>${project.version}</version>
</dependency>
```

Update `@EntityScan` and `@EnableJpaRepositories` base packages if needed.

### Step 7: Update importer/pom.xml

Replace dependency on `fitbit-server` with `fitbit-model`:
```xml
<dependency>
    <groupId>kenny</groupId>
    <artifactId>fitbit-model</artifactId>
    <version>${project.version}</version>
</dependency>
```

Remove the comment about depending on server for entities and repositories.

### Step 8: Update import statements

After moving files, verify all import statements in:
- Server resolvers and exporters (should continue to work as packages unchanged)
- Importer classes (should continue to work as packages unchanged)

### Step 9: Verify builds

```bash
# Clean and build all modules
mvn clean install

# Run server tests
mvn -pl server test

# Run importer tests
mvn -pl importer test
```

### Step 10: Update CLAUDE.md

Update the build instructions to reflect the new module structure:
- Add model module build commands
- Update dependency notes

## Benefits

1. **Clean separation of concerns**: Models/repositories in model, web/GraphQL in server, import logic in importer
2. **Faster importer builds**: No longer pulls in GraphQL, web, and exporter dependencies
3. **Independent development**: Server and importer can evolve independently as long as they don't change model
4. **Clearer architecture**: Dependencies flow one direction from model outward
5. **Reduced coupling**: Breaking the direct server→importer dependency

## Risks and Mitigations

| Risk | Mitigation |
|------|------------|
| Repository queries might need server-specific features | Keep complex queries in repository interfaces; they only depend on Spring Data JPA |
| Cross-package references in ExerciseRepository (imports TimeInHeartRateZones) | Both are in model, so this works |
| Spring Boot auto-configuration might not find repositories | Ensure `@EnableJpaRepositories` scans `kenny.fitbit` package |
| Tests might break if they depend on server classes | Move any shared test utilities to model |

## File Checklist

### Files to CREATE
- [ ] `model/pom.xml`

### Files to MOVE (server → model)
- [ ] `steps/StepsModel.kt`
- [ ] `steps/StepsRepository.kt`
- [ ] `calories/CaloriesModel.kt`
- [ ] `calories/CaloriesRepository.kt`
- [ ] `distance/DistanceModel.kt`
- [ ] `distance/DistanceRepository.kt`
- [ ] `heartrate/HeartRateModel.kt`
- [ ] `heartrate/HeartRateRepository.kt`
- [ ] `exercise/ExerciseModel.kt`
- [ ] `exercise/ExerciseRepository.kt`
- [ ] `sleep/SleepModel.kt`
- [ ] `sleep/SleepRepository.kt`
- [ ] `profile/ProfileModel.kt`
- [ ] `profile/ProfileRepository.kt`

### Files to MODIFY
- [ ] `pom.xml` (parent) - add model module
- [ ] `server/pom.xml` - add model dependency
- [ ] `importer/pom.xml` - replace server dependency with model
- [ ] `CLAUDE.md` - update documentation
