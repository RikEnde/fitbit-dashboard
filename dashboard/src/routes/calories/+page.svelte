<script lang="ts">
    import {onMount} from 'svelte';
    import {client} from '$graphql/client';
    import {DAILY_CALORIES_SUM_QUERY, CALORIES_PER_INTERVAL_QUERY} from '$graphql/queries';
    import {formattedDate, selectedDate, setDate, toLocalISOString} from '$stores/dashboard';
    import {colors} from '$utils/colors';
    import {formatNumber} from '$utils/formatters';
    import {endOfDay, format, parseISO, startOfDay, subDays} from 'date-fns';
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

	interface IntervalRecord {
		timeInterval: string;
		sum: number;
	}

	function processIntervalData(intervals: IntervalRecord[]): { label: string; value: number }[] {
		return intervals.map((interval) => {
			const hour = new Date(interval.timeInterval).getHours();
			return {
				label: `${hour.toString().padStart(2, '0')}:00`,
				value: Math.round(interval.sum)
			};
		});
	}

	async function fetchDailyData(endDate: Date) {
		const range = {
			from: toLocalISOString(startOfDay(subDays(endDate, 29))),
			to: toLocalISOString(endOfDay(endDate))
		};

		const result = await client.query(DAILY_CALORIES_SUM_QUERY, { range }).toPromise();
		if (result.error) {
			throw new Error(result.error.message);
		}

		const rawData = result.data?.dailyCaloriesSum ?? [];
		const dataMap = new Map(rawData.map((d: { date: string; totalCalories: number }) => [d.date, d.totalCalories]));

		// Fill in all 30 days
		const filledData = [];
		for (let i = 29; i >= 0; i--) {
			const date = subDays(endDate, i);
			const dateStr = format(date, 'yyyy-MM-dd');
			filledData.push({
				date: dateStr,
				value: Math.round((dataMap.get(dateStr) as number) ?? 0)
			});
		}

		return filledData;
	}

	async function fetchHourlyData(date: Date) {
		const range = {
			from: toLocalISOString(startOfDay(date)),
			to: toLocalISOString(endOfDay(date))
		};

		const [dailyResult, hourlyResult] = await Promise.all([
			client.query(DAILY_CALORIES_SUM_QUERY, { range }).toPromise(),
			client.query(CALORIES_PER_INTERVAL_QUERY, { range, duration: '1 hour' }).toPromise()
		]);

		if (dailyResult.error) {
			throw new Error(dailyResult.error.message);
		}

		selectedDayTotal = Math.round(dailyResult.data?.dailyCaloriesSum?.[0]?.totalCalories ?? 0);

		if (hourlyResult.data?.caloriesPerInterval) {
			hourlyData = processIntervalData(hourlyResult.data.caloriesPerInterval);
		} else {
			hourlyData = [];
		}
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
		<h1 class="text-2xl font-bold text-theme-text">Calories</h1>
	</div>
	<p class="text-theme-text-secondary mb-8">{$formattedDate}</p>

	{#if loading}
		<div class="space-y-6">
			<div class="bg-theme-card rounded-xl border border-theme-border p-6 animate-pulse">
				<div class="h-6 bg-theme-border rounded w-1/4 mb-4"></div>
				<div class="h-48 bg-theme-border rounded"></div>
			</div>
		</div>
	{:else if error}
		<div class="bg-theme-card rounded-xl border border-theme-border p-6">
			<p class="text-red-400">{error}</p>
		</div>
	{:else}
		<!-- Selected Day Summary -->
		<div class="bg-theme-card rounded-xl border border-theme-border p-6 mb-6">
			<h2 class="text-lg font-semibold text-theme-text mb-4">
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
							<p class="text-2xl font-bold text-theme-text">{formatNumber(selectedDayTotal)}</p>
							<p class="text-xs text-theme-text-secondary">cal</p>
						</div>
					</ProgressRing>
					<div>
						<p class="text-sm text-theme-text-secondary">Goal: {formatNumber(GOAL)}</p>
						<p class="text-sm {percentage >= 100 ? 'text-green-400' : 'text-theme-text-muted'}">
							{percentage}% {percentage >= 100 ? 'complete!' : 'of goal'}
						</p>
					</div>
				</div>

				<!-- Hourly Breakdown -->
				<div class="flex-1">
					<p class="text-sm text-theme-text-secondary mb-2">Hourly Burn</p>
					<MiniBarChart data={hourlyData} color={colors.calories} height={80} showLabels={true} />
				</div>
			</div>
		</div>

		<!-- 30-Day Trend -->
		<div class="bg-theme-card rounded-xl border border-theme-border p-6 mb-6">
			<div class="flex justify-between items-center mb-4">
				<h2 class="text-lg font-semibold text-theme-text">Last 30 Days</h2>
				<div class="text-sm text-theme-text-secondary">
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
			<div class="grid grid-cols-2 md:grid-cols-4 gap-4 mt-6 pt-6 border-t border-theme-border">
				<div>
					<p class="text-xs text-theme-text-muted uppercase tracking-wide">Total</p>
					<p class="text-xl font-bold text-theme-text">{formatNumber(stats.total30Days)}</p>
				</div>
				<div>
					<p class="text-xs text-theme-text-muted uppercase tracking-wide">Daily Average</p>
					<p class="text-xl font-bold text-theme-text">{formatNumber(stats.average30Days)}</p>
				</div>
				<div>
					<p class="text-xs text-theme-text-muted uppercase tracking-wide">Best Day</p>
					<p class="text-xl font-bold text-fitbit-calories">{formatNumber(stats.best30Days)}</p>
				</div>
				<div>
					<p class="text-xs text-theme-text-muted uppercase tracking-wide">Days Met Goal</p>
					<p class="text-xl font-bold {stats.daysMetGoal > 0 ? 'text-green-400' : 'text-theme-text'}">
						{stats.daysMetGoal} / 30
					</p>
				</div>
			</div>
		</div>
	{/if}
</div>
