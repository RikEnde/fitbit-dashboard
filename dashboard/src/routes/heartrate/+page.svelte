<script lang="ts">
    import {onMount} from 'svelte';
    import {gql} from '@urql/svelte';
    import {client} from '$graphql/client';
    import {formattedDate, selectedDate, setDate, toLocalISOString} from '$stores/dashboard';
    import {colors, heartRateZoneColors} from '$utils/colors';
    import {endOfDay, format, parseISO, startOfDay, subDays} from 'date-fns';
    import LineChart from '$components/charts/LineChart.svelte';
    import BarChart from '$components/charts/BarChart.svelte';
    import MiniBarChart from '$components/charts/MiniBarChart.svelte';

    // State
	let loading = $state(true);
	let error = $state<string | null>(null);

	// Current day data
	let heartRateData = $state<{ time: string; value: number }[]>([]);
	let hourlyData = $state<{ label: string; value: number }[]>([]);

	// Stats for current day
	let dayStats = $state({ min: 0, max: 0, avg: 0, current: 0 });

	// Zone distribution for current day
	let zoneDistribution = $state<{ name: string; minutes: number; color: string; percentage: number }[]>([]);

	// 30-day trend data
	let dailyTrend = $state<{ date: string; value: number; min: number; max: number }[]>([]);

	// GraphQL Queries
	const HEART_RATE_QUERY = gql`
		query HeartRates($limit: Int, $range: DateRange) {
			heartRates(limit: $limit, range: $range) {
				id
				bpm
				time
			}
		}
	`;

	interface HeartRateRecord {
		id: string;
		bpm: number;
		time: string;
	}

	// Zone definitions (standard Fitbit zones)
	const zones = [
		{ name: 'Out of Range', min: 0, max: 99, color: heartRateZoneColors['Out of Range'] },
		{ name: 'Fat Burn', min: 100, max: 139, color: heartRateZoneColors['Fat Burn'] },
		{ name: 'Cardio', min: 140, max: 169, color: heartRateZoneColors['Cardio'] },
		{ name: 'Peak', min: 170, max: 999, color: heartRateZoneColors['Peak'] }
	];

	function getZone(bpm: number): string {
		for (const zone of zones) {
			if (bpm >= zone.min && bpm <= zone.max) {
				return zone.name;
			}
		}
		return 'Out of Range';
	}

	function processHourlyData(heartRates: HeartRateRecord[]): { label: string; value: number }[] {
		const hourlyMap = new Map<number, { sum: number; count: number }>();
		for (let i = 0; i < 24; i++) {
			hourlyMap.set(i, { sum: 0, count: 0 });
		}
		for (const hr of heartRates) {
			const hour = new Date(hr.time).getHours();
			const current = hourlyMap.get(hour)!;
			hourlyMap.set(hour, { sum: current.sum + hr.bpm, count: current.count + 1 });
		}
		return Array.from(hourlyMap.entries())
			.sort((a, b) => a[0] - b[0])
			.map(([hour, { sum, count }]) => ({
				label: `${hour.toString().padStart(2, '0')}:00`,
				value: count > 0 ? Math.round(sum / count) : 0
			}));
	}

	function calculateZoneDistribution(heartRates: HeartRateRecord[]): typeof zoneDistribution {
		const zoneCounts = new Map<string, number>();
		zones.forEach((z) => zoneCounts.set(z.name, 0));

		for (const hr of heartRates) {
			const zoneName = getZone(hr.bpm);
			zoneCounts.set(zoneName, (zoneCounts.get(zoneName) ?? 0) + 1);
		}

		const total = heartRates.length;
		return zones.map((zone) => ({
			name: zone.name,
			minutes: zoneCounts.get(zone.name) ?? 0,
			color: zone.color,
			percentage: total > 0 ? ((zoneCounts.get(zone.name) ?? 0) / total) * 100 : 0
		}));
	}

	async function fetchDayData(date: Date) {
		const range = {
			from: toLocalISOString(startOfDay(date)),
			to: toLocalISOString(endOfDay(date))
		};

		const result = await client.query(HEART_RATE_QUERY, { limit: 10000, range }).toPromise();
		if (result.error) {
			throw new Error(result.error.message);
		}

		const heartRates: HeartRateRecord[] = result.data?.heartRates ?? [];

		if (heartRates.length > 0) {
			// Process for line chart (sample every few minutes for smoother chart)
			const sampledData = heartRates
				.filter((_, i) => i % 5 === 0) // Sample every 5th reading
				.map((hr) => ({ time: hr.time, value: hr.bpm }));
			heartRateData = sampledData;

			// Calculate stats
			const bpms = heartRates.map((hr) => hr.bpm);
			dayStats = {
				min: Math.min(...bpms),
				max: Math.max(...bpms),
				avg: Math.round(bpms.reduce((a, b) => a + b, 0) / bpms.length),
				current: heartRates[heartRates.length - 1].bpm
			};

			// Hourly data
			hourlyData = processHourlyData(heartRates);

			// Zone distribution
			zoneDistribution = calculateZoneDistribution(heartRates);
		} else {
			heartRateData = [];
			dayStats = { min: 0, max: 0, avg: 0, current: 0 };
			hourlyData = [];
			zoneDistribution = zones.map((z) => ({ name: z.name, minutes: 0, color: z.color, percentage: 0 }));
		}
	}

	async function fetchTrendData(endDate: Date) {
		const trendData: typeof dailyTrend = [];

		// Fetch 30 days of data - we'll do this in batches for each day
		for (let i = 29; i >= 0; i--) {
			const date = subDays(endDate, i);
			const range = {
				from: toLocalISOString(startOfDay(date)),
				to: toLocalISOString(endOfDay(date))
			};

			const result = await client.query(HEART_RATE_QUERY, { limit: 10000, range }).toPromise();
			const heartRates: HeartRateRecord[] = result.data?.heartRates ?? [];

			if (heartRates.length > 0) {
				const bpms = heartRates.map((hr) => hr.bpm);
				trendData.push({
					date: format(date, 'yyyy-MM-dd'),
					value: Math.round(bpms.reduce((a, b) => a + b, 0) / bpms.length),
					min: Math.min(...bpms),
					max: Math.max(...bpms)
				});
			} else {
				trendData.push({
					date: format(date, 'yyyy-MM-dd'),
					value: 0,
					min: 0,
					max: 0
				});
			}
		}

		dailyTrend = trendData;
	}

	async function fetchAllData() {
		loading = true;
		error = null;

		try {
			const currentDate = $selectedDate;

			// Fetch current day data and trend in parallel
			await Promise.all([
				fetchDayData(currentDate),
				fetchTrendData(currentDate)
			]);
		} catch (e) {
			error = e instanceof Error ? e.message : 'Failed to fetch data';
		} finally {
			loading = false;
		}
	}

	function handleBarClick(dateStr: string) {
		const date = parseISO(dateStr);
		setDate(date);
	}

	let selectedDateStr = $derived(format($selectedDate, 'yyyy-MM-dd'));

	// Stats derived from 30-day trend
	let trendStats = $derived({
		avgHR: dailyTrend.length > 0
			? Math.round(dailyTrend.filter(d => d.value > 0).reduce((sum, d) => sum + d.value, 0) / dailyTrend.filter(d => d.value > 0).length)
			: 0,
		lowestAvg: dailyTrend.length > 0
			? Math.min(...dailyTrend.filter(d => d.value > 0).map(d => d.value))
			: 0,
		highestAvg: dailyTrend.length > 0
			? Math.max(...dailyTrend.filter(d => d.value > 0).map(d => d.value))
			: 0
	});

	onMount(() => {
		fetchAllData();

		const unsubscribe = selectedDate.subscribe((date) => {
			if (!loading) {
				fetchDayData(date);
			}
		});

		return unsubscribe;
	});
</script>

<svelte:head>
	<title>Heart Rate | Fitbit Dashboard</title>
</svelte:head>

<div class="p-4 sm:p-6 lg:p-8">
	<div class="mb-6">
		<a href="/" class="text-fitbit-heartrate hover:underline text-sm">&larr; Back to Dashboard</a>
	</div>

	<div class="flex items-center gap-3 mb-2">
		<svg class="w-8 h-8 text-fitbit-heartrate" fill="currentColor" viewBox="0 0 24 24">
			<path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/>
		</svg>
		<h1 class="text-2xl font-bold text-white">Heart Rate</h1>
	</div>
	<p class="text-gray-400 mb-8">{$formattedDate}</p>

	{#if loading}
		<div class="space-y-6">
			<div class="bg-dark-card rounded-xl border border-dark-border p-6 animate-pulse">
				<div class="h-6 bg-dark-border rounded w-1/4 mb-4"></div>
				<div class="h-48 bg-dark-border rounded"></div>
			</div>
		</div>
	{:else if error}
		<div class="bg-dark-card rounded-xl border border-dark-border p-6">
			<p class="text-red-400">{error}</p>
		</div>
	{:else}
		<!-- Selected Day Summary -->
		<div class="bg-dark-card rounded-xl border border-dark-border p-6 mb-6">
			<h2 class="text-lg font-semibold text-white mb-4">
				{format($selectedDate, 'EEEE, MMMM d')}
			</h2>

			<!-- Stats Row -->
			<div class="grid grid-cols-4 gap-4 mb-6">
				<div class="text-center">
					<p class="text-xs text-gray-500 uppercase tracking-wide">Current</p>
					<p class="text-2xl font-bold text-fitbit-heartrate">{dayStats.current || '--'}</p>
					<p class="text-xs text-gray-500">bpm</p>
				</div>
				<div class="text-center">
					<p class="text-xs text-gray-500 uppercase tracking-wide">Average</p>
					<p class="text-2xl font-bold text-white">{dayStats.avg || '--'}</p>
					<p class="text-xs text-gray-500">bpm</p>
				</div>
				<div class="text-center">
					<p class="text-xs text-gray-500 uppercase tracking-wide">Min</p>
					<p class="text-2xl font-bold text-blue-400">{dayStats.min || '--'}</p>
					<p class="text-xs text-gray-500">bpm</p>
				</div>
				<div class="text-center">
					<p class="text-xs text-gray-500 uppercase tracking-wide">Max</p>
					<p class="text-2xl font-bold text-orange-400">{dayStats.max || '--'}</p>
					<p class="text-xs text-gray-500">bpm</p>
				</div>
			</div>

			<!-- Heart Rate Chart -->
			<div class="mb-6">
				<p class="text-sm text-gray-400 mb-2">Heart Rate Throughout the Day</p>
				<LineChart
					data={heartRateData}
					color={colors.heartrate}
					height={200}
					showArea={true}
					formatValue={(v) => `${v} bpm`}
					minY={40}
					maxY={180}
				/>
			</div>

			<!-- Hourly Average Bar Chart -->
			<div>
				<p class="text-sm text-gray-400 mb-2">Hourly Averages</p>
				<MiniBarChart data={hourlyData} color={colors.heartrate} height={60} showLabels={true} />
			</div>
		</div>

		<!-- Heart Rate Zones -->
		<div class="bg-dark-card rounded-xl border border-dark-border p-6 mb-6">
			<h2 class="text-lg font-semibold text-white mb-4">Heart Rate Zones</h2>
			<p class="text-sm text-gray-400 mb-4">Time spent in each zone today</p>

			<div class="space-y-3">
				{#each zoneDistribution as zone}
					<div class="flex items-center gap-4">
						<div class="w-24 text-sm text-gray-300">{zone.name}</div>
						<div class="flex-1 h-6 bg-dark-border rounded-full overflow-hidden">
							<div
								class="h-full rounded-full transition-all duration-500"
								style="width: {zone.percentage}%; background-color: {zone.color};"
							></div>
						</div>
						<div class="w-20 text-right text-sm text-gray-400">
							{zone.minutes} min ({Math.round(zone.percentage)}%)
						</div>
					</div>
				{/each}
			</div>
		</div>

		<!-- 30-Day Trend -->
		<div class="bg-dark-card rounded-xl border border-dark-border p-6">
			<div class="flex justify-between items-center mb-4">
				<h2 class="text-lg font-semibold text-white">Last 30 Days</h2>
				<div class="text-sm text-gray-400">
					Daily average heart rate
				</div>
			</div>

			<BarChart
				data={dailyTrend}
				color={colors.heartrate}
				height={200}
				formatValue={(v) => `${v} bpm`}
				onBarClick={handleBarClick}
				selectedDate={selectedDateStr}
			/>

			<!-- Stats Grid -->
			<div class="grid grid-cols-3 gap-4 mt-6 pt-6 border-t border-dark-border">
				<div>
					<p class="text-xs text-gray-500 uppercase tracking-wide">30-Day Average</p>
					<p class="text-xl font-bold text-white">{trendStats.avgHR} <span class="text-sm text-gray-400">bpm</span></p>
				</div>
				<div>
					<p class="text-xs text-gray-500 uppercase tracking-wide">Lowest Avg Day</p>
					<p class="text-xl font-bold text-blue-400">{trendStats.lowestAvg} <span class="text-sm text-gray-400">bpm</span></p>
				</div>
				<div>
					<p class="text-xs text-gray-500 uppercase tracking-wide">Highest Avg Day</p>
					<p class="text-xl font-bold text-orange-400">{trendStats.highestAvg} <span class="text-sm text-gray-400">bpm</span></p>
				</div>
			</div>
		</div>
	{/if}
</div>
