# Fitbit Kotlin Application

This application provides a GraphQL API for querying Fitbit data stored in a PostgreSQL database.

## GraphQL API

The application exposes a GraphQL API at `/graphql` that allows querying various types of Fitbit data:

- Heart rate data (raw data and aggregated by interval)
- Steps data (raw data, daily sums, and weekly averages)
- Calories data
- Distance data
- Exercise data
- Sleep data
- Sleep score data
- Profile data
- Health status

### GraphiQL

The application includes GraphiQL, a web-based GraphQL IDE, which is accessible at `/graphiql` when the application is running.

### Example Queries

Here are some example GraphQL queries:

#### Query Heart Rate Data

```graphql
query {
  heartRates(limit: 10, offset: 0) {
    id
    bpm
    confidence
    time
  }
}
```

#### Query Exercise Data with Heart Rate Zones

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

#### Query Sleep Data with Level Summaries

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

#### Query Profile Data

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

#### Query Aggregated Steps Data

```graphql
query {
  dailyStepsSum(range: {from: "2024-01-01T00:00:00Z", to: "2024-01-31T23:59:59Z"}) {
    date
    totalSteps
  }

  weeklyStepsAverage(range: {from: "2024-01-01T00:00:00Z", to: "2024-03-31T23:59:59Z"}) {
    weekNumber
    averageSteps
  }
}
```

#### Query Heart Rate Data by Interval

```graphql
query {
  heartRatesPerInterval(range: {from: "2024-01-01T00:00:00Z", to: "2024-01-01T23:59:59Z"}) {
    timeInterval
    bpmSum
  }
}
```

#### Health Check Query

```graphql
query {
  healthStatus {
    status
    message
  }
}
```

## REST API

In addition to the GraphQL API, the application also exposes a REST API at `/api` using Spring Data REST. This provides standard CRUD operations for all entities.

## Running the Application

1. Create a `.env` file in the root directory by copying the `.env.example` file:
   ```
   cp .env.example .env
   ```
   Then edit the `.env` file to set your desired database credentials.

2. Start PostgreSQL and pgAdmin using Docker Compose:
   ```
   docker-compose up -d
   ```

3. Run the application using Maven: `./mvnw spring-boot:run`
4. Access the GraphiQL interface at http://localhost:8080/graphiql
5. Access the REST API at http://localhost:8080/api
6. Access the GraphQL schema at http://localhost:8080/graphql/schema
