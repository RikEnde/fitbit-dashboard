# Agents Guide - Fitbit Dashboard

This file provides guidance for AI agents working on the dashboard codebase.

## Tech Stack

- **Framework**: SvelteKit 2 with Svelte 5 (runes mode)
- **Language**: TypeScript
- **Styling**: TailwindCSS with custom Fitbit color palette
- **GraphQL**: URQL client
- **Date handling**: date-fns

## Svelte 5 Runes Mode

This project uses Svelte 5 runes. **Do NOT use legacy Svelte syntax.**

### Correct Patterns

```svelte
<script lang="ts">
  // State - use $state()
  let count = $state(0);
  let items = $state<string[]>([]);

  // Derived values - use $derived()
  let doubled = $derived(count * 2);
  let hasItems = $derived(items.length > 0);

  // Props - use $props()
  interface Props {
    title: string;
    count?: number;
  }
  let { title, count = 0 }: Props = $props();

  // Effects - use $effect()
  $effect(() => {
    console.log('Count changed:', count);
  });
</script>
```

### Incorrect Patterns (DO NOT USE)

```svelte
<!-- WRONG - legacy syntax -->
<script>
  let count = 0;           // Use $state(0) instead
  $: doubled = count * 2;  // Use $derived() instead
  export let title;        // Use $props() instead
</script>
```

## Component Structure

Components are organized in `src/lib/components/`:

```
components/
├── layout/          # Header, ProfileAvatar, ProfileDropdown
├── tiles/           # Dashboard tiles (StepsTile, CaloriesTile, etc.)
├── charts/          # Reusable chart components
│   ├── ProgressRing.svelte      # Circular goal progress indicator
│   ├── MiniBarChart.svelte      # Small hourly bar chart for tiles
│   ├── BarChart.svelte          # Full 30-day trend bar chart with click interaction
│   ├── LineChart.svelte         # SVG line/area chart for heart rate
│   └── SleepStagesChart.svelte  # Sleep stages timeline visualization
└── common/          # Shared UI components (TileWrapper, etc.)
```

## Detail Pages

Detail pages are in `src/routes/`:

| Route | Description |
|-------|-------------|
| `/steps` | 30-day trend, hourly breakdown, weekly averages table |
| `/heartrate` | Day line chart, zones distribution, 30-day trend |
| `/sleep` | Stages timeline, score breakdown, 30-day trend |
| `/exercise` | Activity list with expandable HR zones, 30-day trend |
| `/calories` | 30-day trend, hourly breakdown, goal progress |
| `/distance` | 30-day trend, hourly breakdown (converts cm to km) |
| `/profile` | User info, physical stats, stride lengths, unit preferences |

## GraphQL Queries

### SSR Compatibility

URQL's `queryStore` does not work during SSR. Always fetch data in `onMount`:

```svelte
<script lang="ts">
  import { onMount } from 'svelte';
  import { gql } from '@urql/svelte';
  import { client } from '$graphql/client';

  let data = $state(null);
  let loading = $state(true);
  let error = $state<string | null>(null);

  const MY_QUERY = gql`
    query MyQuery($range: DateRange!) {
      myData(range: $range) {
        id
        value
      }
    }
  `;

  onMount(() => {
    // Subscribe to store changes for reactive refetching
    const unsubscribe = someStore.subscribe(async (value) => {
      loading = true;
      const result = await client.query(MY_QUERY, { range: value }).toPromise();
      if (result.error) {
        error = result.error.message;
      } else {
        data = result.data;
      }
      loading = false;
    });
    return unsubscribe;
  });
</script>
```

### Available Queries

The GraphQL backend provides these queries:

| Query | Purpose |
|-------|---------|
| `steps(limit, range)` | Per-minute step data |
| `dailyStepsSum(range)` | Daily step totals |
| `calories(limit, range)` | Per-minute calorie data |
| `distances(limit, range)` | Per-minute distance (in cm!) |
| `heartRates(limit, range)` | Per-minute heart rate |
| `sleeps(limit, range)` | Sleep logs with stages |
| `sleepScores(limit, range)` | Sleep scores |
| `exercises(limit, range)` | Exercise sessions |
| `profiles(limit)` | User profile data |

## Data Units

**Important**: Some data requires unit conversion:

| Data Type | Stored Unit | Display Unit | Conversion |
|-----------|-------------|--------------|------------|
| Distance | centimeters | kilometers | ÷ 100,000 |
| Stride length | centimeters | inches | ÷ 2.54 |
| Exercise duration | milliseconds | minutes | ÷ 60,000 |
| Sleep duration | minutes | hours + minutes | formatDuration() |

## Stores

Stores are in `src/lib/stores/`:

- **dashboard.ts**: Selected date, date range, navigation helpers
- **preferences.ts**: User preferences (visible tiles, layout)
- **profile.ts**: User profile data

### Date Range Store

```typescript
import { dateRange, selectedDate, setDate, goToNextDay, goToPreviousDay } from '$stores/dashboard';

// Subscribe to date changes
dateRange.subscribe((range) => {
  // range = { from: ISO string, to: ISO string }
  fetchData(range);
});

// Navigate dates
setDate(new Date('2024-12-27'));
goToNextDay();
goToPreviousDay();
```

## Creating a New Tile

1. Create `src/lib/components/tiles/MyTile.svelte`:

```svelte
<script lang="ts">
  import { onMount } from 'svelte';
  import { gql } from '@urql/svelte';
  import { client } from '$graphql/client';
  import { dateRange } from '$stores/dashboard';
  import { colors } from '$utils/colors';
  import ProgressRing from '$components/charts/ProgressRing.svelte';
  import MiniBarChart from '$components/charts/MiniBarChart.svelte';

  interface Props {
    goal?: number;
  }
  let { goal = 100 }: Props = $props();

  let total = $state(0);
  let loading = $state(true);
  let error = $state<string | null>(null);
  let hourlyData = $state<{ label: string; value: number }[]>([]);

  const QUERY = gql`...`;

  async function fetchData(range: { from: string; to: string }) {
    loading = true;
    error = null;
    try {
      const result = await client.query(QUERY, { range }).toPromise();
      if (result.error) {
        error = result.error.message;
        return;
      }
      // Process result.data
    } catch (e) {
      error = e instanceof Error ? e.message : 'Failed to fetch';
    } finally {
      loading = false;
    }
  }

  onMount(() => {
    const unsubscribe = dateRange.subscribe(fetchData);
    return unsubscribe;
  });

  let percentage = $derived(goal > 0 ? Math.round((total / goal) * 100) : 0);
</script>

<a href="/mypage" class="tile block cursor-pointer group">
  <!-- Tile content -->
</a>

<style>
  .tile {
    @apply bg-dark-card rounded-xl p-4 border border-dark-border transition-all duration-200;
  }
  .tile:hover {
    @apply border-gray-500 shadow-lg;
  }
</style>
```

2. Export from `src/lib/components/tiles/index.ts`
3. Add to `src/routes/+page.svelte`

## Creating a Detail Page

Detail pages follow a common pattern. Create `src/routes/mymetric/+page.svelte`:

```svelte
<script lang="ts">
  import { onMount } from 'svelte';
  import { gql } from '@urql/svelte';
  import { client } from '$graphql/client';
  import { selectedDate, setDate, formattedDate } from '$stores/dashboard';
  import { colors } from '$utils/colors';
  import { formatNumber } from '$utils/formatters';
  import { startOfDay, endOfDay, subDays, format, parseISO } from 'date-fns';
  import BarChart from '$components/charts/BarChart.svelte';
  import MiniBarChart from '$components/charts/MiniBarChart.svelte';
  import ProgressRing from '$components/charts/ProgressRing.svelte';

  const GOAL = 10000;
  let loading = $state(true);
  let error = $state<string | null>(null);
  let dailyData = $state<{ date: string; value: number }[]>([]);
  let hourlyData = $state<{ label: string; value: number }[]>([]);
  let selectedDayTotal = $state(0);

  // Fetch 30-day data, aggregate by day
  async function fetchDailyData(endDate: Date) { ... }

  // Fetch hourly breakdown for selected day
  async function fetchHourlyData(date: Date) { ... }

  function handleBarClick(dateStr: string) {
    const date = parseISO(dateStr);
    setDate(date);
    fetchHourlyData(date);
  }

  onMount(() => {
    fetchAllData();
    const unsubscribe = selectedDate.subscribe((date) => {
      if (!loading) fetchHourlyData(date);
    });
    return unsubscribe;
  });
</script>
```

Key elements:
- **Back link**: `<a href="/">← Back to Dashboard</a>`
- **Day summary card**: ProgressRing + hourly MiniBarChart
- **30-day trend**: BarChart with `onBarClick` for date selection
- **Stats grid**: Total, average, best day, days met goal

## Color Palette

Use the Fitbit color constants from `$utils/colors`:

```typescript
import { colors } from '$utils/colors';

colors.steps      // '#00B0B9' - teal
colors.calories   // '#FF6B35' - orange
colors.distance   // '#7C3AED' - purple
colors.active     // '#22C55E' - green
colors.heartrate  // '#EF4444' - red
colors.sleep      // '#6366F1' - indigo
```

Or use Tailwind classes: `text-fitbit-steps`, `bg-fitbit-calories`, etc.

## Common Issues

### "Cannot read properties of undefined (reading 'createRequestOperation')"

This happens when using `queryStore` during SSR. Use `onMount` with `client.query().toPromise()` instead.

### Bars not rendering in MiniBarChart

The chart uses pixel heights. Ensure parent has explicit height and data has non-zero values.

### Dates showing wrong data

The API stores dates without timezone. Ensure you're querying the correct date range and the user's Fitbit data exists for that date.

## Running the Dashboard

```bash
cd dashboard
npm run dev    # http://localhost:3000
```

The backend must be running on port 8080:
```bash
mvn -pl server spring-boot:run
```
