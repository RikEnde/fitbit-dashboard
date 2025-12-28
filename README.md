# Fitbit Kotlin Application

A Kotlin/Spring Boot application for importing, storing, and querying your Fitbit data. Includes a GraphQL API, REST API, and React frontend for visualizing health metrics.

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

### 5. Run the React Client (Optional)

```bash
cd client
npm install
npm start
```

The client runs on http://localhost:3000 and proxies API requests to the server.

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
