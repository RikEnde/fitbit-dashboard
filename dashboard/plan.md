# Fitbit Dashboard Recreation Plan

## Overview

This document outlines the design and implementation plan for recreating the original Fitbit web dashboard, which was discontinued by Google on July 8, 2024. The goal is to provide a familiar, tile-based interface that displays fitness and health data from our existing GraphQL backend.

## Background & Motivation

Google's decision to shut down the Fitbit web dashboard was met with significant user criticism. Key complaints included:

- **Screen size**: Users found it much easier to view and interact with health data on a larger screen
- **Missing features**: The mobile app lacks capabilities that were available on the web (meal creation, pace charts, workout logging, achievement info)
- **Accessibility**: As users age, viewing metrics on a larger screen becomes increasingly important
- **Data exploration**: The web dashboard offered more flexibility for viewing specific date ranges

### Original Dashboard Characteristics

The original Fitbit dashboard featured:
- **Tile-based layout** similar to Windows 8 tiles - glanceable and customizable
- **Drag-and-drop reordering** of tiles
- **Show/hide tiles** based on user preference
- **Quick access** to: Steps, Calories, Sleep, Heart Rate, Active Minutes, Distance, Floors
- **Drill-down capability** - click a tile to see detailed data
- **Date range selection** for historical analysis

---

## Available Data

The database contains comprehensive Fitbit data across all domains. **We will create GraphQL queries and resolvers as needed** - the API should serve the dashboard's needs, not constrain them.

### Core Metrics (Primary Tiles)
| Metric | Database Entity | Data Granularity |
|--------|-----------------|------------------|
| **Steps** | `Steps` | Per-minute readings |
| **Heart Rate** | `HeartRate` | Per-minute BPM with confidence |
| **Calories** | `Calories` | Per-minute values |
| **Distance** | `Distance` | Per-minute values |
| **Sleep** | `Sleep`, `SleepLevelData`, `SleepLevelSummary` | Full sleep logs with stage breakdowns |
| **Exercise** | `Exercise`, `HeartRateZone`, `ActivityLevel` | Activity sessions with HR zones |

### Advanced Metrics (Secondary Tiles)
| Metric | Database Entity | Data Available |
|--------|-----------------|----------------|
| **Sleep Score** | `SleepScore` | Overall, Composition, Revitalization, Duration scores |
| **Resting Heart Rate** | `RestingHeartRate` | Daily values with error margins |
| **HRV** | `DailyHeartRateVariability`, `HeartRateVariabilityDetails` | RMSSD, Entropy, Low/High frequency |
| **SpO2** | `MinuteSpO2`, `DailySpO2` | Minute-level and daily averages |
| **Respiratory Rate** | `DailyRespiratoryRate`, `RespiratoryRateSummary` | Daily and per-sleep-stage rates |
| **Body Temperature** | `DeviceTemperature`, `ComputedTemperature` | Device readings, Computed sleep temps |
| **VO2 Max** | `DemographicVO2Max`, `RunVO2Max` | Demographic and run-specific values |
| **Activity Minutes** | `ActivityMinutes` | Sedentary, Light, Moderate, Active breakdowns |
| **Activity Goals** | `ActivityGoal` | Targets, progress, and status |
| **Time in HR Zones** | `TimeInHeartRateZones` | Daily breakdown by zone |
| **Profile** | `Profile` | User info, units, goals, avatar |

---

## Technology Evaluation

Evaluating purely on technical merits for a dashboard application with tiles, charts, and GraphQL integration.

### Option 1: React

**Pros:**
- Largest ecosystem for data visualization (Recharts, Victory, Nivo, Visx)
- Mature GraphQL clients (Apollo, URQL, TanStack Query + graphql-request)
- Excellent TypeScript support
- Most AI training data - reliable code generation
- Extensive component libraries (Radix, Headless UI, shadcn/ui)

**Cons:**
- More boilerplate code (useState, useEffect patterns)
- Virtual DOM overhead
- JSX mixing logic and markup can get verbose
- Requires more dependencies for common patterns

### Option 2: Vue.js

**Pros:**
- Single-file components (.vue) - clean separation of template/script/style
- Built-in reactivity without boilerplate
- Excellent TypeScript support (Vue 3)
- Good visualization options (Vue-ChartJS, Vue-ECharts, ApexCharts)
- GraphQL via Apollo or Villus (Vue-native)
- Gentler complexity curve

**Cons:**
- Smaller visualization ecosystem than React
- Template syntax is another thing to generate correctly
- Two-way binding can complicate data flow debugging

### Option 3: Svelte/SvelteKit (Recommended)

**Pros:**
- **60-70% smaller bundle sizes** - compiles away the framework
- **Cleanest syntax** - least boilerplate of any framework
- **True reactivity** - Svelte 5 runes (`$state`, `$derived`, `$effect`) are intuitive
- No virtual DOM - direct DOM updates = fastest runtime
- Built-in transitions and animations
- SvelteKit provides routing, SSR, and build tooling out of the box
- Good GraphQL support via @urql/svelte or custom fetch
- LayerChart and Pancake for visualization (or use D3 directly)
- Excellent for dashboards - fast updates for real-time data

**Cons:**
- Smaller ecosystem than React
- Fewer pre-built component libraries
- May need to wrap D3 or Chart.js manually for complex charts

### Option 4: Solid.js

**Pros:**
- React-like JSX syntax but with fine-grained reactivity
- Excellent performance (comparable to Svelte)
- No virtual DOM
- Easy migration path if familiar with React patterns

**Cons:**
- Smallest ecosystem of all options
- Fewer visualization libraries
- Less AI training data available

### Comparison Matrix

| Criteria | React | Vue | Svelte | Solid |
|----------|-------|-----|--------|-------|
| Bundle Size | Large | Medium | Small | Small |
| Runtime Performance | Good | Good | Excellent | Excellent |
| Code Simplicity | Verbose | Clean | Cleanest | Clean |
| Visualization Libraries | Excellent | Good | Adequate | Limited |
| GraphQL Integration | Excellent | Good | Good | Good |
| TypeScript Support | Excellent | Excellent | Good | Excellent |
| Component Libraries | Excellent | Good | Growing | Limited |

### Recommendation

**SvelteKit** is recommended for this project because:

1. **Cleanest code** - Less boilerplate means easier to generate, read, and maintain
2. **Best performance** - Compile-time framework with no virtual DOM overhead
3. **Smallest bundles** - Important for a dashboard that may load many charts
4. **Built-in routing & SSR** - SvelteKit provides everything needed out of the box
5. **Excellent for dashboards** - Fast reactive updates for data visualization
6. **D3 integration** - Can use D3.js directly for any chart type without wrapper limitations

For data visualization, we'll use:
- **LayerChart** - Svelte-native charting library built on D3
- **D3.js** directly for custom visualizations
- **TailwindCSS** for styling

For GraphQL:
- **@urql/svelte** - Lightweight GraphQL client with Svelte bindings
- Or simple `fetch` + `graphql-request` for maximum simplicity

---

## Design Specifications

### Layout Philosophy

The dashboard should follow these principles:
1. **Glanceable**: Key metrics visible at a glance without scrolling
2. **Tile-based**: Modular tiles that can be rearranged
3. **Responsive**: Works on desktop, tablet, and large displays
4. **Drill-down**: Click any tile to see detailed data
5. **Customizable**: Users can show/hide tiles

### Page Structure

```
+------------------------------------------------------------------+
|  Header: Logo | Date Selector | Settings | [Avatar]              |
+------------------------------------------------------------------+
|                                                                  |
|  +------------+  +------------+  +------------+  +------------+  |
|  |   STEPS    |  |  CALORIES  |  |  DISTANCE  |  |   ACTIVE   |  |
|  |   12,345   |  |   2,456    |  |   6.2 mi   |  |  MINUTES   |  |
|  |  Goal: 10k |  |  Goal: 2k  |  |            |  |    45      |  |
|  |  [======]  |  |  [======]  |  |            |  |  [======]  |  |
|  +------------+  +------------+  +------------+  +------------+  |
|                                                                  |
|  +---------------------------+  +---------------------------+    |
|  |       HEART RATE          |  |         SLEEP             |    |
|  |   Current: 72 bpm         |  |   Last night: 7h 23m      |    |
|  |   Resting: 58 bpm         |  |   Score: 82               |    |
|  |   [~~~~~ graph ~~~~~]     |  |   [==== sleep stages ==]  |    |
|  +---------------------------+  +---------------------------+    |
|                                                                  |
|  +---------------------------+  +---------------------------+    |
|  |       EXERCISE            |  |      SLEEP SCORE          |    |
|  |   Today: Run - 45 min     |  |   [circular gauge: 82]    |    |
|  |   This week: 3 activities |  |   Composition | Duration  |    |
|  +---------------------------+  +---------------------------+    |
|                                                                  |
+------------------------------------------------------------------+
```

### Tile Types

#### 1. Metric Tile (Small)
- Single value display with optional goal progress
- Progress bar or ring showing goal completion
- Trend indicator (up/down arrow)

#### 2. Chart Tile (Medium)
- Mini chart (line or bar) showing recent trend
- Time period selector (day/week/month)
- Click to expand to full detail view

#### 3. Summary Tile (Large)
- Multiple related metrics
- More detailed visualization
- Sub-components (e.g., sleep stages breakdown)

### Header Components

#### Logo
- Fitbit-style logo or custom branding
- Click to return to dashboard home

#### Date Selector
- Current date display with calendar picker
- Previous/Next day navigation arrows
- Quick jumps: Today, Yesterday, Last Week

#### Settings Menu
- Show/hide tiles toggle
- Dark/Light mode switch
- Unit preferences (if not from profile)

#### Profile Avatar
- Circular mini avatar (32x32px) from user profile
- Positioned at right edge of header
- Click to open profile dropdown/popover

**Profile Dropdown Contents:**
```
+--------------------------------+
|  [Large Avatar]                |
|  Display Name                  |
|  email@example.com             |
+--------------------------------+
|  Member since: Jan 2020        |
|  Age: 35 | Height: 5'10"       |
|  Weight: 165 lbs               |
+--------------------------------+
|  Stride: Walk 2.5ft / Run 3.2ft|
|  Daily Goals:                  |
|    Steps: 10,000               |
|    Calories: 2,500             |
|    Active Min: 30              |
+--------------------------------+
|  [Settings] [Sign Out]         |
+--------------------------------+
```

**Profile Data Displayed:**
- Avatar image (from profile.avatar, base64 JPEG)
- Full name and display name
- Email address
- Member since date
- Physical stats: age (calculated from DOB), height, weight
- Stride lengths (walking/running)
- Daily goals (steps, calories, active minutes, distance)
- Unit preferences (distance, weight, height)

### Color Scheme

Following Fitbit's established color language:
- **Steps**: Teal/Cyan (`#00B0B9`)
- **Calories**: Orange (`#FF6B35`)
- **Distance**: Purple (`#7C3AED`)
- **Active Minutes**: Green (`#22C55E`)
- **Heart Rate**: Red/Pink (`#EF4444`)
- **Sleep**: Indigo/Blue (`#6366F1`)
- **Background**: Dark (`#1a1a2e`) or Light mode option

### Responsive Breakpoints

| Breakpoint | Layout |
|------------|--------|
| Desktop (>1200px) | 4-column grid |
| Tablet (768-1200px) | 2-column grid |
| Mobile (<768px) | 1-column stack |

---

## Feature Specifications

### Phase 1: Core Dashboard (MVP)

#### 1.1 Main Dashboard View
- [ ] Tile grid layout with drag-drop reordering (using svelte-grid-extended)
- [x] Date selector in header (single day view) with calendar picker
- [ ] Show/hide tiles menu
- [x] Persist layout preferences in localStorage

#### 1.2 Steps Tile
- [x] Daily step count with goal progress ring
- [x] Mini bar chart showing hourly breakdown
- [x] Click to navigate to detailed Steps view

#### 1.3 Heart Rate Tile
- [x] Current/latest heart rate reading
- [x] Min/max/avg heart rate stats
- [x] Mini bar chart of hourly averages
- [x] Heart rate zones indicator
- [x] Click to navigate to detailed HR view

#### 1.4 Sleep Tile
- [x] Last night's sleep duration
- [x] Sleep efficiency percentage
- [x] Sleep stages mini-visualization (horizontal bar)
- [x] Click to navigate to detailed Sleep view

#### 1.5 Calories Tile
- [x] Daily calorie burn with goal progress
- [x] Hourly breakdown mini bar chart
- [x] Click for detailed view

#### 1.6 Distance Tile
- [x] Daily distance traveled (converted from cm to km)
- [x] Goal progress ring
- [x] Click for detailed view

#### 1.7 Active Minutes Tile
- [x] Total active minutes with goal progress
- [x] Exercise count and calories burned
- [x] Recent activities list

#### 1.8 Profile Avatar & Dropdown
- [x] Mini avatar icon in header (32x32px circular)
- [x] Fetch profile data on app load
- [x] Click avatar to open dropdown popover
- [x] Display user info: name, email, member since
- [x] Display physical stats: height, weight, age
- [x] Display stride lengths and daily goals
- [x] Show unit preferences from profile

### Phase 2: Detailed Views ✅ COMPLETE

#### 2.1 Steps Detail Page ✅
- [x] Daily bar chart (last 30 days)
- [x] Weekly averages display
- [x] Hourly breakdown for selected day
- [x] Interactive date selection via chart
- [ ] Export functionality

#### 2.2 Heart Rate Detail Page ✅
- [x] Line chart for selected day
- [x] Hourly averages display
- [x] Heart rate zones distribution
- [x] 30-day trend chart
- [x] Interactive date selection

#### 2.3 Sleep Detail Page ✅
- [x] Sleep duration and efficiency display
- [x] Sleep stages timeline visualization
- [x] Sleep score with component breakdown
- [x] 30-day trend chart
- [x] Interactive date selection

#### 2.4 Exercise Detail Page ✅
- [x] Exercise log list with expandable details
- [x] Activity type breakdown chart
- [x] Heart rate zones per activity
- [x] Calories and stats per exercise
- [x] 30-day active minutes trend

#### 2.5 Calories Detail Page ✅
- [x] Daily bar chart (last 30 days)
- [x] Hourly breakdown for selected day
- [x] Goal progress display
- [x] Interactive date selection

#### 2.6 Distance Detail Page ✅
- [x] Daily bar chart (last 30 days)
- [x] Hourly breakdown for selected day
- [x] Unit display (km)
- [x] Interactive date selection

#### 2.7 Profile Page ✅
- [x] Large avatar and full name display
- [x] Account info (member since, timezone)
- [x] Personal info (age, gender, height, weight)
- [x] Stride lengths (walking/running)
- [x] Unit preferences
- [x] Theme toggle
- [x] Link from profile dropdown

### Phase 3: Advanced Features

#### 3.1 Additional Tiles
- [ ] SpO2 tile (blood oxygen)
- [ ] Respiratory rate tile
- [ ] Body temperature tile
- [ ] VO2 Max tile
- [ ] HRV tile

#### 3.2 Insights & Trends
- [ ] Weekly/Monthly summary view
- [ ] Year-over-year comparisons
- [ ] Personal records tracking
- [ ] Correlation analysis (e.g., sleep vs. activity)

#### 3.3 Data Export
- [ ] CSV export for any metric
- [ ] Date range selection for exports
- [ ] Apple Health XML export (already supported by backend)

#### 3.4 User Experience
- [ ] Dark/Light mode toggle
- [ ] Keyboard navigation
- [ ] Print-friendly views
- [ ] PWA support for offline access

---

## Technical Architecture

### Project Structure

```
dashboard/
├── src/
│   ├── lib/
│   │   ├── components/
│   │   │   ├── layout/
│   │   │   │   ├── Header.svelte
│   │   │   │   ├── TileGrid.svelte
│   │   │   │   ├── ProfileAvatar.svelte
│   │   │   │   └── ProfileDropdown.svelte
│   │   │   ├── tiles/
│   │   │   │   ├── StepsTile.svelte
│   │   │   │   ├── HeartRateTile.svelte
│   │   │   │   ├── SleepTile.svelte
│   │   │   │   ├── CaloriesTile.svelte
│   │   │   │   ├── DistanceTile.svelte
│   │   │   │   └── ActiveMinutesTile.svelte
│   │   │   ├── charts/
│   │   │   │   ├── BarChart.svelte         # 30-day trend charts
│   │   │   │   ├── LineChart.svelte        # Heart rate day view
│   │   │   │   ├── ProgressRing.svelte     # Goal progress rings
│   │   │   │   ├── MiniBarChart.svelte     # Tile hourly charts
│   │   │   │   ├── SleepStagesChart.svelte # Sleep stages timeline
│   │   │   │   └── index.ts                # Barrel exports
│   │   │   └── common/
│   │   │       ├── DatePicker.svelte
│   │   │       ├── TileWrapper.svelte
│   │   │       └── LoadingSpinner.svelte
│   │   ├── graphql/
│   │   │   ├── queries.ts
│   │   │   └── client.ts
│   │   ├── stores/
│   │   │   ├── profile.ts        # Svelte store for profile data
│   │   │   ├── dashboard.ts      # Svelte store for dashboard state
│   │   │   └── preferences.ts    # Layout & theme preferences
│   │   ├── utils/
│   │   │   ├── dateUtils.ts
│   │   │   ├── formatters.ts
│   │   │   └── colors.ts
│   │   └── types/
│   │       └── index.ts
│   ├── routes/
│   │   ├── +layout.svelte        # Root layout with header
│   │   ├── +page.svelte          # Dashboard home
│   │   ├── steps/
│   │   │   └── +page.svelte      # Steps detail page
│   │   ├── heartrate/
│   │   │   └── +page.svelte      # Heart rate detail page
│   │   ├── sleep/
│   │   │   └── +page.svelte      # Sleep detail page
│   │   └── exercise/
│   │       └── +page.svelte      # Exercise detail page
│   ├── app.html
│   ├── app.css                   # Global styles + Tailwind
│   └── app.d.ts
├── static/
│   └── favicon.png
├── package.json
├── svelte.config.js
├── tailwind.config.js
├── vite.config.ts
└── README.md
```

### Key Dependencies

```json
{
  "devDependencies": {
    "@sveltejs/adapter-static": "^3.0.6",
    "@sveltejs/kit": "^2.9.0",
    "@sveltejs/vite-plugin-svelte": "^5.0.0",
    "svelte": "^5.12.0",
    "svelte-check": "^4.1.1",
    "typescript": "^5.7.2",
    "vite": "^6.0.3",
    "tailwindcss": "^3.4.16",
    "autoprefixer": "^10.4.20",
    "postcss": "^8.4.49"
  },
  "dependencies": {
    "@urql/svelte": "^4.2.1",
    "graphql": "^16.9.0",
    "d3": "^7.9.0",
    "date-fns": "^4.1.0"
  }
}
```

### Svelte 5 Patterns (Runes Mode)

**Reactivity** - Svelte 5 runes replace the old `$:` syntax:
```svelte
<script lang="ts">
  let count = $state(0);
  let doubled = $derived(count * 2);  // Automatically updates when count changes
</script>
```

**Props** - Component props use `$props()`:
```svelte
<script lang="ts">
  interface Props {
    goal?: number;
  }
  let { goal = 10000 }: Props = $props();
</script>
```

**Stores** - Shared state without prop drilling:
```typescript
// stores/profile.ts
import { writable } from 'svelte/store';
export const profile = writable(null);

// In component - auto-subscribes with $ prefix
<p>Welcome, {$profile?.displayName}</p>
```

**GraphQL with URQL** - Use client queries in onMount to avoid SSR issues:
```svelte
<script lang="ts">
  import { onMount } from 'svelte';
  import { gql } from '@urql/svelte';
  import { client } from '$graphql/client';

  let data = $state(null);
  let loading = $state(true);

  onMount(async () => {
    const result = await client.query(MY_QUERY, { variables }).toPromise();
    data = result.data;
    loading = false;
  });
</script>

{#if loading}
  <LoadingSpinner />
{:else}
  <StepsTile steps={data.steps} />
{/if}
```

---

## Development Milestones

### Milestone 1: Project Setup (Foundation) ✅ COMPLETE

**Frontend:**
- [x] Initialize SvelteKit project with TypeScript (Svelte 5 runes mode)
- [x] Configure Tailwind CSS with Fitbit color palette
- [x] Set up URQL client for GraphQL
- [x] Create root layout with header component
- [x] Implement responsive layout shell
- [x] Add date selector with calendar picker
- [x] Add profile avatar with dropdown

**Backend:**
- No changes needed for initial setup

### Milestone 2: Core Tiles ✅ COMPLETE

**Frontend:**
- [x] Create TileWrapper component
- [x] Build Svelte store for layout preferences (persisted to localStorage)
- [x] Steps tile with goal progress ring and hourly bar chart
- [x] Calories tile with goal progress and hourly breakdown
- [x] Distance tile with km conversion (from cm) and progress ring
- [x] Heart rate tile with min/max/avg stats and hourly chart
- [x] Sleep tile with duration, stages bar, and efficiency
- [x] Active minutes tile with exercise count and recent activities
- [x] ProgressRing and MiniBarChart reusable components

**Remaining:**
- [ ] Implement svelte-grid-extended for drag-drop tiles
- [ ] Create tile show/hide menu

**Backend:**
- Uses existing queries (steps, calories, distances, heartRates, sleeps, exercises)
- [ ] Implement `dailySummary` query for optimization (optional)

### Milestone 3: Detail Views & Trend APIs ✅ COMPLETE (Frontend)

**Frontend:** ✅
- [x] Steps detail page with charts
- [x] Heart rate detail page
- [x] Sleep detail page
- [x] Exercise log page

**Backend:** (Optional optimization - current queries work fine)
- [ ] Implement trend queries: `dailyCaloriesTrend`, `dailyDistanceTrend`, `dailySleepTrend`
- [ ] Add `restingHeartRateTrend` query
- [ ] Add `sleepScoreTrend` query
- [ ] Optimize queries for large date ranges

### Milestone 4: Polish & Advanced

**Frontend:**
- [ ] Dark/Light mode toggle
- [ ] Additional metric tiles (SpO2, HRV, Temperature)
- [ ] Data export functionality
- [ ] PWA configuration

**Backend:**
- [ ] Add queries for advanced metrics (SpO2, HRV, respiratory rate)
- [ ] CSV export endpoint (complement existing Apple Health XML export)

---

## Open Questions

1. **Goal data**: Do we have user goals stored in the database, or should we make them configurable in the UI?

2. **Real-time updates**: Should the dashboard poll for updates, or is refresh-on-demand sufficient?

3. **Multi-profile support**: Should the dashboard support switching between profiles?

4. **Units**: Should we support metric/imperial toggle, or derive from profile settings?

5. **Historical comparison**: Should tiles show comparison to yesterday/last week by default?

---

## Required GraphQL API

The dashboard requires specific queries optimized for its use cases. We will implement these in the server module as needed. This is a **design-first** approach - the API serves the dashboard.

### Dashboard Overview Queries (New)

These queries power the main dashboard tiles with single requests:

```graphql
type Query {
  # Daily summary for a single date - powers main dashboard
  dailySummary(date: Date!): DailySummary

  # Latest readings for real-time display
  latestHeartRate: HeartRate
  latestSleep: Sleep
}

type DailySummary {
  date: Date!
  steps: DailyStepsData
  calories: DailyCaloriesData
  distance: DailyDistanceData
  activeMinutes: DailyActiveMinutesData
  heartRate: DailyHeartRateData
  sleep: SleepSummary
}

type DailyStepsData {
  total: Int!
  goal: Int
  hourlyBreakdown: [HourlyValue!]!
  percentageOfGoal: Float
}

type DailyCaloriesData {
  total: Float!
  goal: Float
  hourlyBreakdown: [HourlyValue!]!
}

type DailyDistanceData {
  total: Float!
  unit: DistanceUnit!
  hourlyBreakdown: [HourlyValue!]!
}

type DailyActiveMinutesData {
  total: Int!
  goal: Int
  sedentary: Int!
  light: Int!
  moderate: Int!
  active: Int!
}

type DailyHeartRateData {
  current: Int
  resting: Float
  min: Int!
  max: Int!
  average: Float!
  zones: [HeartRateZoneSummary!]!
}

type SleepSummary {
  duration: Int          # minutes
  efficiency: Int
  score: Int
  stages: SleepStages
}

type SleepStages {
  awake: Int!
  light: Int!
  deep: Int!
  rem: Int!
}

type HourlyValue {
  hour: Int!            # 0-23
  value: Float!
}

type HeartRateZoneSummary {
  name: String!
  minutes: Int!
  percentage: Float!
}
```

### Trend Queries (New)

For detail pages and historical analysis:

```graphql
type Query {
  # Multi-day aggregations for charts
  dailyStepsTrend(range: DateRange!): [DailyStepsSum!]!
  dailyCaloriesTrend(range: DateRange!): [DailyCaloriesSum!]!
  dailyDistanceTrend(range: DateRange!): [DailyDistanceSum!]!
  dailySleepTrend(range: DateRange!): [DailySleepSum!]!
  dailyActiveMinutesTrend(range: DateRange!): [DailyActiveMinutesSum!]!

  # Weekly aggregations
  weeklyStepsAverage(range: DateRange!): [WeeklyAverage!]!
  weeklyCaloriesAverage(range: DateRange!): [WeeklyAverage!]!

  # Resting heart rate trend
  restingHeartRateTrend(range: DateRange!): [RestingHeartRatePoint!]!

  # Sleep score trend
  sleepScoreTrend(range: DateRange!): [SleepScorePoint!]!
}

type DailyCaloriesSum {
  date: Date!
  total: Float!
}

type DailyDistanceSum {
  date: Date!
  total: Float!
  unit: DistanceUnit!
}

type DailySleepSum {
  date: Date!
  duration: Int!
  efficiency: Int
  score: Int
}

type DailyActiveMinutesSum {
  date: Date!
  total: Int!
  moderate: Int!
  active: Int!
}

type WeeklyAverage {
  weekStart: Date!
  average: Float!
}

type RestingHeartRatePoint {
  date: Date!
  value: Float!
}

type SleepScorePoint {
  date: Date!
  score: Int!
}
```

### Existing Queries to Leverage

These existing queries can be used as-is or with minor modifications:

| Query | Use Case | Status |
|-------|----------|--------|
| `dailyStepsSum` | Steps trend chart | Exists |
| `weeklyStepsAverage` | Weekly comparison | Exists |
| `heartRates` | Detailed HR chart | Exists |
| `heartRatesPerInterval` | HR aggregation | Exists |
| `sleeps` | Sleep detail view | Exists |
| `sleepScores` | Sleep score display | Exists |
| `exercises` | Exercise log | Exists |
| `profile` | User settings, units, avatar dropdown | Exists |

### Backend Implementation Notes

New resolvers should follow the existing patterns in `server/src/main/kotlin/kenny/fitbitkotlin/`:

1. **Resolver location**: `{domain}/{Domain}Resolver.kt`
2. **Use `@QueryMapping`** annotation for GraphQL queries
3. **Aggregation queries** should use native SQL via `@Query` in repositories for performance
4. **Pagination** with `limit`/`offset` for large datasets
5. **Date filtering** via `DateRange` input type

Example resolver pattern:
```kotlin
@Controller
class DashboardResolver(
    private val stepsRepository: StepsRepository,
    private val caloriesRepository: CaloriesRepository,
    // ... other repositories
) {
    @QueryMapping
    fun dailySummary(@Argument date: LocalDate): DailySummary {
        // Aggregate data from multiple repositories
    }
}
```

---

## References

- [Google shuts down Fitbit web dashboard (PhoneArena)](https://www.phonearena.com/news/google-shuts-down-fitbit-web-dashboard_id159323)
- [Today's your last day to use Fitbit's web dashboard (Android Police)](https://www.androidpolice.com/fitbit-web-dashboard-shut-down/)
- [Bring back the Fitbit Web Dashboard (Fitbit Community)](https://community.fitbit.com/t5/Product-Feedback/Bring-back-the-Fitbit-Web-Dashboard/idi-p/5689990)
- [Dashboard Customization (Robots.net)](https://robots.net/computing-and-gadgets/wearables/dashboard-customization-adding-tiles-to-your-fitbit-dashboard/)
- [Frontend Frameworks Comparison 2025](https://medium.com/@ignatovich.dm/react-vs-vue-vs-svelte-choosing-the-right-framework-for-2025-4f4bb9da35b4)
- [Third-party Fitbit Web UI (GitHub)](https://github.com/arpanghosh8453/fitbit-web-ui-app)
