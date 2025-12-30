# Fitbit Kotlin Application

A Kotlin/Spring Boot application for importing, storing, and querying your Fitbit data. Includes a GraphQL API, REST API,
and SvelteKit dashboard for visualizing health metrics.

The goals of this project are two-fold.
First, I wanted to be able to preserve and view my historical Fitbit data after moving to a tracker from another brand.
Second, I wanted to do this as much as possible using AI tools, without writing any code by hand. 

It took some planning and correcting to make the AI stick to the architecture and not always take the easiest path to 
implement a feature.
We have to keep in mind that a significant part of architecture is to keep the code readable and 
maintainable by humans.
I can imagine future architectures that are designed never to be touched by humans may favor 
very different choices, but as of late 2025 we are not there yet. 

There was a significant leap in capabilities of the AI tools between when the project was started in early 2024 and when 
it was finished. 

## Obtaining Your Fitbit Data

To use this application, you first need to download your data from Fitbit:

1. Log in to your Fitbit account at [fitbit.com](https://www.fitbit.com)
2. Go to **Settings** (gear icon) → **Data Export**
3. Or navigate directly to: https://www.fitbit.com/settings/data/export
4. Click **Request Data** to export your complete Fitbit history
5. Fitbit will email you when your data is ready (this can take a few hours to days)
6. Download the ZIP file and extract it to a `data` directory in the parent folder of this project:
   ```
   ../data/
   ├── Personal & Account/
   │   ├── Profile.csv
   │   └── Media/
   ├── Physical Activity/
   │   ├── heart_rate-2024-01-01.json
   │   ├── steps-2024-01-01.json
   │   └── ...
   ├── Sleep/
   │   ├── sleep-2024-01-01.json
   │   └── ...
   └── ...
   ```

### Import Your Fitbit Data

```bash
# Import all data types
mvn -pl importer spring-boot:run -Dspring-boot.run.arguments="--heartrate --steps --calories --distance --exercise --sleep --sleepscore --profile"

# Or import specific data types
mvn -pl importer spring-boot:run -Dspring-boot.run.arguments="--heartrate --steps"
```

Supported import options:
- `--heartrate` - Heart rate measurements
- `--steps` - Step counts
- `--calories` - Calories burned
- `--distance` - Distance traveled
- `--exercise` - Exercise/activity logs
- `--sleep` - Sleep sessions
- `--sleepscore` - Sleep scores
- `--restingheartrate` - Resting heart rate
- `--timeinzone` - Time in heart rate zones
- `--activityminutes` - Activity minutes
- `--activezoneminutes` - Active zone minutes
- `--vo2max` - VO2 Max estimates
- `--runvo2max` - Running VO2 Max
- `--activitygoals` - Activity goals
- `--devicetemperature` - Device temperature
- `--respiratoryrate` - Respiratory rate
- `--hrv` - Heart rate variability
- `--hrvdetails` - HRV details
- `--minutespo2` - SpO2 minutes
- `--computedtemperature` - Computed temperature
- `--respiratoryratesummary` - Respiratory rate summary
- `--dailyspo2` - Daily SpO2
- `--profile` - User profile

### 3. Run the Server

```bash
mvn -pl server spring-boot:run
```

The server will start on http://localhost:8080

### 4. Access the Application

- **GraphiQL** (GraphQL IDE): http://localhost:8080/graphiql
- **REST API**: http://localhost:8080/api
- **pgAdmin** (database admin): http://localhost:5050

### 5. Run the Dashboard (Optional)

```bash
cd dashboard
npm install
npm run dev
```

The dashboard runs on http://localhost:3000 and proxies API requests to the server.

## GraphQL API

The application exposes a GraphQL API at `/graphql` for querying Fitbit data.

### Example Queries

#### Heart Rate Data

```graphql
# Get recent heart rate readings
query {
  heartRates(limit: 10, offset: 0) {
    id
    bpm
    confidence
    time
  }
}

# Get heart rate aggregated by time interval
query {
  heartRatesPerInterval(range: {from: "2024-01-01T00:00:00Z", to: "2024-01-01T23:59:59Z"}) {
    timeInterval
    bpmSum
  }
}
```

#### Steps Data

```graphql
# Get daily step totals
query {
  dailyStepsSum(range: {from: "2024-01-01T00:00:00Z", to: "2024-01-31T23:59:59Z"}) {
    date
    totalSteps
  }
}

# Get weekly step averages
query {
  weeklyStepsAverage(range: {from: "2024-01-01T00:00:00Z", to: "2024-03-31T23:59:59Z"}) {
    weekNumber
    averageSteps
  }
}
```

#### Exercise Data

```graphql
query {
  exercises(limit: 5, offset: 0) {
    id
    activityName
    calories
    duration
    startTime
    heartRateZones {
      name
      min
      max
      minutes
    }
    activityLevels {
      name
      minutes
    }
  }
}
```

#### Sleep Data

```graphql
query {
  sleeps(limit: 5, offset: 0) {
    id
    logId
    dateOfSleep
    startTime
    endTime
    minutesAsleep
    minutesAwake
    levelSummaries {
      level
      minutes
      thirtyDayAvgMinutes
    }
  }
}
```

#### Profile Data

```graphql
query {
  profiles(limit: 5) {
    id
    displayName
    emailAddress
    gender
    height
    weight
  }

  profile(id: "1") {
    id
    fullName
    dateOfBirth
    memberSince
  }
}
```

#### Health Check

```graphql
query {
  healthStatus {
    status
    message
  }
}
```

## REST API

The application also exposes a REST API at `/api` using Spring Data REST, providing standard CRUD operations for all entities.

## Data Export

Export your data to Apple Health XML format (compatible with iOS "Health Data Importer" app):

```bash
# Export heart rate data
curl "http://localhost:8080/api/export/heartrate?from=2024-01-01T00:00:00&to=2024-12-31T23:59:59" -o heartrate.xml

# Export steps data
curl "http://localhost:8080/api/export/steps?from=2024-01-01T00:00:00&to=2024-12-31T23:59:59" -o steps.xml
```

Available export types: `heartrate`, `steps`, `calories`, `distance`, `sleep`

The amount of heart rate data for an entire year can be too large for Apple Health to import in one go, so you may have
to divide the export data into chunks. See the script `export.sh` for an example.

## Architecture

The project consists of four main modules:

- **model** - Shared JPA entities and Spring Data repositories (used by server and importer)
- **server** - REST API and GraphQL server with resolvers and exporters
- **importer** - Data import CLI for Fitbit JSON/CSV files
- **dashboard** - SvelteKit web dashboard for visualizing Fitbit data

```
        model
       /     \
      v       v
   server   importer
```

## Tech Stack

- Kotlin 2.3.0 / JVM 25 / Spring Boot 3.4.4
- PostgreSQL 17 with JPA/Hibernate
- GraphQL + REST (Spring Data REST)
- SvelteKit 2 + Svelte 5 + TypeScript + URQL + TailwindCSS
