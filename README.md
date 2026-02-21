# Fitbit Kotlin Application

A Kotlin/Spring Boot application for importing, storing, and querying your Fitbit data. Includes a GraphQL API, REST API,
and SvelteKit dashboard for visualizing health metrics.

The goals of this project are two-fold.
First, I wanted to be able to preserve and view my historical Fitbit data after moving to a tracker from another brand. 
Second, I wanted to do this as much as possible using AI tools, without writing any code by hand.

The data turned out to be a strange mix of relational and unstructured data. The choice was made to import the data into 
a database, and make a web application with exposing the data via GraphQL, to support querying the data as well as 
support a web UI with graphs, to replace the old Fitbit dashboard. 

It took some planning and correcting to make the AI stick to the architecture and not always take the easiest path to 
implement a feature. We have to keep in mind that a significant part of architecture is to keep the code readable and 
maintainable by humans. I can imagine future architectures that are designed never to be touched by humans may favor 
very different choices, but as of late 2025 we are not there yet. 

There was a significant leap in capabilities of the AI tools between when the project was started in early 2025 and when 
it was finished. By spring 2025 the tools were just starting to become useful, and by the end of the year it was clear 
that being a "programmer", either as a profession or as an identity, is going to way of weavers and blacksmiths. 

## Disclaimer

If you decide to run this application for yourself, do not expose it to the internet. This is your personal data. 
Fitbit records when you sleep, when you exercise, your GPS locations (though we are not importing that data). I think 
it should be obvious why you need to keep this private. The security / authentication serves only to support multiple 
users, it is not designed to protect your data when exposed to the internet.

I accept no responsibility for any consequences of using this software or the information in this documentation.


## Obtaining Your Fitbit Data

To use this application, you first need to download your data from Fitbit:

1. Log in to your Fitbit account at [fitbit.com](https://www.fitbit.com)
2. Go to **Settings** (gear icon) → **Data Export**
3. Or navigate directly to: https://www.fitbit.com/settings/data/export
4. Click **Request Data** to export your complete Fitbit history
5. Fitbit will email you when your data is ready (this can take a few hours to days)
6. Download the ZIP file. You can either:
   - **Upload it directly** via the dashboard (Import Data) or the REST API
   - **Extract it** to a `data` directory for CLI import (see below)

   For CLI import, extract the zip so each user's data goes in a subdirectory named after the user (the directory name becomes the username):
   ```
   ../data/
   └── YourName/
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
# Import all data types for all users (data directory defaults to ../data)
mvn -pl importer-cli spring-boot:run -Dspring-boot.run.arguments="--all"

# Import specific data types
mvn -pl importer-cli spring-boot:run -Dspring-boot.run.arguments="--heartrate --steps"

# Import for a specific user
mvn -pl importer-cli spring-boot:run -Dspring-boot.run.arguments="--all --user=YourName"

# Import from a custom data directory
mvn -pl importer-cli spring-boot:run -Dspring-boot.run.arguments="--all --datadir=/path/to/data"
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
- `--all` - All of the above
- `--user=NAME` - Import only the specified user (default: all users)
- `--datadir=PATH` - Data directory (default: `../data`)

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

### Data Import via REST

You can trigger data imports through the server's REST API by uploading a Fitbit export zip file:

```bash
curl -u user:password -X POST http://localhost:8080/api/import \
  -F "file=@MyFitbitData.zip" \
  -F "stats=all"
```

Parameters:
- `file` - The Fitbit data export zip file (required)
- `stats` - Comma-separated stat types to import, or `all` for everything (default: `all`)

The response returns a job ID. Poll `GET /api/import/{jobId}` for progress and results.

You can also import via the dashboard: click your profile avatar and select **Import Data**.

## Data Export

Only Apple Health XML format is currently supported. 

The Apple Health API, also known as HealthKit is only available on iOS, so we can't directly upload the data. This 
application can export the stats to files in the Apple Health XML format, which you can import into Apple Health using 
one of several available (paid) apps in the app store. I'm not affiliated with any of them. They're fine, the ones I 
tested all worked. 

### Via Dashboard

Log in to the dashboard, click your profile avatar, and select **Export to Apple Health**. Choose a data type and date 
range, then click Export to download the XML file.

### Via CLI

```bash
# Export heart rate data
curl -u user:password "http://localhost:8080/api/export/heartrate?from=2024-01-01T00:00:00&to=2024-12-31T23:59:59" -o heartrate.xml

# Export steps data
curl -u user:password "http://localhost:8080/api/export/steps?from=2024-01-01T00:00:00&to=2024-12-31T23:59:59" -o steps.xml
```

Available export types: `heartrate`, `steps`, `calories`, `distance`, `sleep`

The amount of heart rate data for an entire year can be too large for Apple Health to import in one go, so you may have
to divide the export data into chunks. See the script `export.sh` for an example.

## Architecture

The project consists of five modules:

- **model** - Shared JPA entities and Spring Data repositories
- **importer** - Importer library: base classes and domain importers (no `@SpringBootApplication`, plain jar)
- **importer-cli** - CLI runner for the importer (`@SpringBootApplication`, `ImportRunner`, executable jar)
- **server** - REST API, GraphQL server, resolvers, exporters, and REST import endpoint (depends on importer library)
- **dashboard** - SvelteKit web dashboard for visualizing Fitbit data

Dependencies:
- **importer** depends on model
- **importer-cli** depends on importer
- **server** depends on model and importer

## Tech Stack

- Kotlin 2.3.0 / JVM 25 / Spring Boot 3.4.4
- PostgreSQL 17 with JPA/Hibernate
- GraphQL + REST (Spring Data REST)
- SvelteKit 2 + Svelte 5 + TypeScript + URQL + TailwindCSS
