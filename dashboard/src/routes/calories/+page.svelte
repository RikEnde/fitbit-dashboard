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

	// Constants
	const GOAL = 2500;

	// State
	let loading = $state(true);
	let error = $state<string | null>(null);

	// Daily data for 30-day chart
	let dailyData = $state<{ date: string; value: number }[]>([]);

	// Hourly data for selected day
	let hourlyData = $state<{ label: string; value: number }[]>([]);

	// Selected day stats
	let selectedDayTotal = $state(0);

	// Summary stats
	let stats = $derived({
		total30Days: dailyData.reduce((sum, d) => sum + d.value, 0),
		average30Days: dailyData.length > 0 ? Math.round(dailyData.reduce((sum, d) => sum + d.value, 0) / dailyData.length) : 0,
		best30Days: dailyData.length > 0 ? Math.max(...dailyData.map((d) => d.value)) : 0,
		daysMetGoal: dailyData.filter((d) => d.value >= GOAL).length
	});

	// GraphQL Query
	const CALORIES_QUERY = gql`
		query Calories($limit: Int, $range: DateRange) {
			calories(limit: $limit, range: $range) {
				id
				value
				dateTime
			}
		}
	`;

	interface CaloriesRecord {
		id: string;
		value: number;
		dateTime: string;
	}

	function processHourlyData(records: CaloriesRecord[]): { label: string; value: number }[] {
		const hourlyMap = new Map<number, number>();
		for (let i = 0; i < 24; i++) {
			hourlyMap.set(i, 0);
		}
		for (const record of records) {
			const hour = new Date(record.dateTime).getHours();
			hourlyMap.set(hour, (hourlyMap.get(hour) ?? 0) + record.value);
		}
		return Array.from(hourlyMap.entries())
			.sort((a, b) => a[0] - b[0])
			.map(([hour, value]) => ({
				label: `${hour.toString().padStart(2, '0')}:00`,
				value: Math.round(value)
			}));
	}

	function aggregateByDay(records: CaloriesRecord[]): Map<string, number> {
		const dailyMap = new Map<string, number>();
		for (const record of records) {
			const date = format(new Date(record.dateTime), 'yyyy-MM-dd');
			dailyMap.set(date, (dailyMap.get(date) ?? 0) + record.value);
		}
		return dailyMap;
	}

	async function fetchDailyData(endDate: Date) {
		const range = {
			from: startOfDay(subDays(endDate, 29)).toISOString(),
			to: endOfDay(endDate).toISOString()
		};

		const result = await client.query(CALORIES_QUERY, { limit: 50000, range }).toPromise();
		if (result.error) {
			throw new Error(result.error.message);
		}

		const records: CaloriesRecord[] = result.data?.calories ?? [];
		const dataMap = aggregateByDay(records);

		// Fill in all 30 days
		const filledData = [];
		for (let i = 29; i >= 0; i--) {
			const date = subDays(endDate, i);
			const dateStr = format(date, 'yyyy-MM-dd');
			filledData.push({
				date: dateStr,
				value: Math.round(dataMap.get(dateStr) ?? 0)
			});
		}

		return filledData;
	}

	async function fetchHourlyData(date: Date) {
		const range = {
			from: startOfDay(date).toISOString(),
			to: endOfDay(date).toISOString()
		};

		const result = await client.query(CALORIES_QUERY, { limit: 1440, range }).toPromise();
		if (result.error) {
			throw new Error(result.error.message);
		}

		const records: CaloriesRecord[] = result.data?.calories ?? [];
		selectedDayTotal = Math.round(records.reduce((sum, r) => sum + r.value, 0));
		hourlyData = processHourlyData(records);
	}

	async function fetchAllData() {
		loading = true;
		error = null;

		try {
			const currentDate = $selectedDate;
			dailyData = await fetchDailyData(currentDate);
			await fetchHourlyData(currentDate);
		} catch (e) {
			error = e instanceof Error ? e.message : 'Failed to fetch data';
		} finally {
			loading = false;
		}
	}

	function handleBarClick(dateStr: string) {
		const date = parseISO(dateStr);
		setDate(date);
		fetchHourlyData(date);
	}

	let selectedDateStr = $derived(format($selectedDate, 'yyyy-MM-dd'));
	let percentage = $derived(GOAL > 0 ? Math.round((selectedDayTotal / GOAL) * 100) : 0);

	onMount(() => {
		fetchAllData();

		const unsubscribe = selectedDate.subscribe((date) => {
			if (!loading) {
				fetchHourlyData(date);
			}
		});

		return unsubscribe;
	});
</script>

<svelte:head>
	<title>Calories | Fitbit Dashboard</title>
</svelte:head>

<div class="p-4 sm:p-6 lg:p-8">
	<div class="mb-6">
		<a href="/" class="text-fitbit-calories hover:underline text-sm">&larr; Back to Dashboard</a>
	</div>

	<div class="flex items-center gap-3 mb-2">
		<svg class="w-8 h-8 text-fitbit-calories" fill="currentColor" viewBox="0 0 24 24">
			<path d="M11 21c0-1.1-.9-2-2-2H5v-2h4c1.1 0 2-.9 2-2s-.9-2-2-2H5v-2h4c1.1 0 2-.9 2-2s-.9-2-2-2H5V5h6c1.1 0 2-.9 2-2s-.9-2-2-2H3v22h8c1.1 0 2-.9 2-2z M21 3c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2s2-.9 2-2V5c0-1.1-.9-2-2-2z M17 7c-1.1 0-2 .9-2 2v8c0 1.1.9 2 2 2s2-.9 2-2V9c0-1.1-.9-2-2-2z"/>
		</svg>
		<h1 class="text-2xl font-bold text-white">Calories</h1>
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
			<div class="flex flex-col md:flex-row gap-6">
				<!-- Progress Ring -->
				<div class="flex items-center gap-4">
					<ProgressRing
						value={selectedDayTotal}
						max={GOAL}
						size={120}
						strokeWidth={10}
						color={colors.calories}
					>
						<div class="text-center">
							<p class="text-2xl font-bold text-white">{formatNumber(selectedDayTotal)}</p>
							<p class="text-xs text-gray-400">cal</p>
						</div>
					</ProgressRing>
					<div>
						<p class="text-sm text-gray-400">Goal: {formatNumber(GOAL)}</p>
						<p class="text-sm {percentage >= 100 ? 'text-green-400' : 'text-gray-500'}">
							{percentage}% {percentage >= 100 ? 'complete!' : 'of goal'}
						</p>
					</div>
				</div>

				<!-- Hourly Breakdown -->
				<div class="flex-1">
					<p class="text-sm text-gray-400 mb-2">Hourly Burn</p>
					<MiniBarChart data={hourlyData} color={colors.calories} height={80} showLabels={true} />
				</div>
			</div>
		</div>

		<!-- 30-Day Trend -->
		<div class="bg-dark-card rounded-xl border border-dark-border p-6 mb-6">
			<div class="flex justify-between items-center mb-4">
				<h2 class="text-lg font-semibold text-white">Last 30 Days</h2>
				<div class="text-sm text-gray-400">
					Click a bar to view details
				</div>
			</div>
			<BarChart
				data={dailyData}
				color={colors.calories}
				height={200}
				goal={GOAL}
				formatValue={formatNumber}
				onBarClick={handleBarClick}
				selectedDate={selectedDateStr}
			/>

			<!-- Stats Grid -->
			<div class="grid grid-cols-2 md:grid-cols-4 gap-4 mt-6 pt-6 border-t border-dark-border">
				<div>
					<p class="text-xs text-gray-500 uppercase tracking-wide">Total</p>
					<p class="text-xl font-bold text-white">{formatNumber(stats.total30Days)}</p>
				</div>
				<div>
					<p class="text-xs text-gray-500 uppercase tracking-wide">Daily Average</p>
					<p class="text-xl font-bold text-white">{formatNumber(stats.average30Days)}</p>
				</div>
				<div>
					<p class="text-xs text-gray-500 uppercase tracking-wide">Best Day</p>
					<p class="text-xl font-bold text-fitbit-calories">{formatNumber(stats.best30Days)}</p>
				</div>
				<div>
					<p class="text-xs text-gray-500 uppercase tracking-wide">Days Met Goal</p>
					<p class="text-xl font-bold {stats.daysMetGoal > 0 ? 'text-green-400' : 'text-white'}">
						{stats.daysMetGoal} / 30
					</p>
				</div>
			</div>
		</div>
	{/if}
</div>
