<script lang="ts">
    import {onMount} from 'svelte';
    import {gql} from '@urql/svelte';
    import {client} from '$graphql/client';
    import {formattedDate, selectedDate, setDate} from '$stores/dashboard';
    import {colors} from '$utils/colors';
    import {formatNumber} from '$utils/formatters';
    import {endOfDay, endOfWeek, format, parseISO, startOfDay, startOfWeek, subDays, subWeeks} from 'date-fns';
    import BarChart from '$components/charts/BarChart.svelte';
    import MiniBarChart from '$components/charts/MiniBarChart.svelte';
    import ProgressRing from '$components/charts/ProgressRing.svelte';

    // Props
	const GOAL = 10000;

	// State
	let loading = $state(true);
	let error = $state<string | null>(null);

	// Daily data for 30-day chart
	let dailyData = $state<{ date: string; value: number }[]>([]);

	// Weekly averages
	let weeklyData = $state<{ weekNumber: string; averageSteps: number }[]>([]);

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

	// GraphQL Queries
	const DAILY_STEPS_SUM_QUERY = gql`
		query DailyStepsSum($range: DateRange!) {
			dailyStepsSum(range: $range) {
				date
				totalSteps
			}
		}
	`;

	const WEEKLY_STEPS_AVERAGE_QUERY = gql`
		query WeeklyStepsAverage($range: DateRange!) {
			weeklyStepsAverage(range: $range) {
				weekNumber
				averageSteps
			}
		}
	`;

	const HOURLY_STEPS_QUERY = gql`
		query Steps($limit: Int, $range: DateRange) {
			steps(limit: $limit, range: $range) {
				id
				value
				dateTime
			}
		}
	`;

	interface StepRecord {
		id: string;
		value: number;
		dateTime: string;
	}

	function processHourlyData(steps: StepRecord[]): { label: string; value: number }[] {
		const hourlyMap = new Map<number, number>();
		for (let i = 0; i < 24; i++) {
			hourlyMap.set(i, 0);
		}
		for (const step of steps) {
			const hour = new Date(step.dateTime).getHours();
			hourlyMap.set(hour, (hourlyMap.get(hour) ?? 0) + step.value);
		}
		return Array.from(hourlyMap.entries())
			.sort((a, b) => a[0] - b[0])
			.map(([hour, value]) => ({
				label: `${hour.toString().padStart(2, '0')}:00`,
				value
			}));
	}

	async function fetchDailyData(endDate: Date) {
		const range = {
			from: startOfDay(subDays(endDate, 29)).toISOString(),
			to: endOfDay(endDate).toISOString()
		};

		const result = await client.query(DAILY_STEPS_SUM_QUERY, { range }).toPromise();
		if (result.error) {
			throw new Error(result.error.message);
		}

		const rawData = result.data?.dailyStepsSum ?? [];
		// Create a map for easy lookup
		const dataMap = new Map(rawData.map((d: { date: string; totalSteps: number }) => [d.date, d.totalSteps]));

		// Fill in all 30 days
		const filledData = [];
		for (let i = 29; i >= 0; i--) {
			const date = subDays(endDate, i);
			const dateStr = format(date, 'yyyy-MM-dd');
			filledData.push({
				date: dateStr,
				value: (dataMap.get(dateStr) as number) ?? 0
			});
		}

		return filledData;
	}

	async function fetchWeeklyData(endDate: Date) {
		// Get 12 weeks of data
		const range = {
			from: startOfWeek(subWeeks(endDate, 11)).toISOString(),
			to: endOfWeek(endDate).toISOString()
		};

		const result = await client.query(WEEKLY_STEPS_AVERAGE_QUERY, { range }).toPromise();
		if (result.error) {
			throw new Error(result.error.message);
		}

		return result.data?.weeklyStepsAverage ?? [];
	}

	async function fetchHourlyData(date: Date) {
		const range = {
			from: startOfDay(date).toISOString(),
			to: endOfDay(date).toISOString()
		};

		const [dailyResult, hourlyResult] = await Promise.all([
			client.query(DAILY_STEPS_SUM_QUERY, { range }).toPromise(),
			client.query(HOURLY_STEPS_QUERY, { limit: 1440, range }).toPromise()
		]);

		if (dailyResult.error) {
			throw new Error(dailyResult.error.message);
		}

		selectedDayTotal = dailyResult.data?.dailyStepsSum?.[0]?.totalSteps ?? 0;

		if (hourlyResult.data?.steps) {
			hourlyData = processHourlyData(hourlyResult.data.steps);
		} else {
			hourlyData = [];
		}
	}

	async function fetchAllData() {
		loading = true;
		error = null;

		try {
			const currentDate = $selectedDate;

			const [daily, weekly] = await Promise.all([
				fetchDailyData(currentDate),
				fetchWeeklyData(currentDate)
			]);

			dailyData = daily;
			weeklyData = weekly;

			// Also fetch hourly data for the selected day
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

		// Re-fetch when selected date changes
		const unsubscribe = selectedDate.subscribe((date) => {
			// Only re-fetch hourly data, not the full 30 days
			if (!loading) {
				fetchHourlyData(date);
			}
		});

		return unsubscribe;
	});
</script>

<svelte:head>
	<title>Steps | Fitbit Dashboard</title>
</svelte:head>

<div class="p-4 sm:p-6 lg:p-8">
	<div class="mb-6">
		<a href="/" class="text-fitbit-steps hover:underline text-sm">&larr; Back to Dashboard</a>
	</div>

	<div class="flex items-center gap-3 mb-2">
		<svg class="w-8 h-8 text-fitbit-steps" fill="currentColor" viewBox="0 0 24 24">
			<path d="M13.5 5.5c1.09 0 2-.81 2-1.81s-.91-1.69-2-1.69-2 .59-2 1.59.91 1.91 2 1.91zM17.5 10.78c-1.23-.89-2.62-1.35-4.05-1.35-.71 0-1.4.11-2.05.32L10 7.5c-.2-.55-.7-.94-1.28-.94-.83 0-1.5.67-1.5 1.5 0 .19.04.38.11.55l2.44 5.94c.22.53.64.94 1.14 1.14L11 16.5v4c0 .83.67 1.5 1.5 1.5s1.5-.67 1.5-1.5v-4.88l1.87-4.68c.25-.63-.02-1.34-.56-1.66z"/>
		</svg>
		<h1 class="text-2xl font-bold text-white">Steps</h1>
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
						color={colors.steps}
					>
						<div class="text-center">
							<p class="text-2xl font-bold text-white">{formatNumber(selectedDayTotal)}</p>
							<p class="text-xs text-gray-400">steps</p>
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
					<p class="text-sm text-gray-400 mb-2">Hourly Activity</p>
					<MiniBarChart data={hourlyData} color={colors.steps} height={80} showLabels={true} />
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
				color={colors.steps}
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
					<p class="text-xl font-bold text-fitbit-steps">{formatNumber(stats.best30Days)}</p>
				</div>
				<div>
					<p class="text-xs text-gray-500 uppercase tracking-wide">Days Met Goal</p>
					<p class="text-xl font-bold {stats.daysMetGoal > 0 ? 'text-green-400' : 'text-white'}">
						{stats.daysMetGoal} / 30
					</p>
				</div>
			</div>
		</div>

		<!-- Weekly Averages -->
		{#if weeklyData.length > 0}
			<div class="bg-dark-card rounded-xl border border-dark-border p-6">
				<h2 class="text-lg font-semibold text-white mb-4">Weekly Averages</h2>
				<div class="overflow-x-auto">
					<table class="w-full">
						<thead>
							<tr class="text-left text-xs text-gray-500 uppercase tracking-wide">
								<th class="pb-3">Week</th>
								<th class="pb-3 text-right">Average Steps</th>
								<th class="pb-3 text-right">vs Goal</th>
							</tr>
						</thead>
						<tbody class="text-white">
							{#each weeklyData as week}
								{@const avg = Math.round(week.averageSteps)}
								{@const vsGoal = avg - GOAL}
								<tr class="border-t border-dark-border">
									<td class="py-3 text-sm">{week.weekNumber}</td>
									<td class="py-3 text-right font-medium">{formatNumber(avg)}</td>
									<td class="py-3 text-right text-sm {vsGoal >= 0 ? 'text-green-400' : 'text-red-400'}">
										{vsGoal >= 0 ? '+' : ''}{formatNumber(vsGoal)}
									</td>
								</tr>
							{/each}
						</tbody>
					</table>
				</div>
			</div>
		{/if}
	{/if}
</div>
