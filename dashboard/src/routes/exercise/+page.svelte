<script lang="ts">
    import {onMount} from 'svelte';
    import {gql} from '@urql/svelte';
    import {client} from '$graphql/client';
    import {formattedDate, selectedDate, setDate, toLocalISOString} from '$stores/dashboard';
    import {colors, heartRateZoneColors} from '$utils/colors';
    import {formatDuration, formatNumber} from '$utils/formatters';
    import {endOfDay, format, parseISO, startOfDay, subDays} from 'date-fns';
    import ProgressRing from '$components/charts/ProgressRing.svelte';
    import BarChart from '$components/charts/BarChart.svelte';

    // Constants
	const ACTIVE_MINUTES_GOAL = 30;

	// State
	let loading = $state(true);
	let error = $state<string | null>(null);

	// Current day's exercises
	let exercises = $state<ExerciseRecord[]>([]);

	// 30-day trend
	let dailyTrend = $state<{ date: string; value: number; count: number }[]>([]);

	// Expanded exercise for details
	let expandedId = $state<string | null>(null);

	// GraphQL Query
	const EXERCISE_QUERY = gql`
		query Exercises($limit: Int, $range: DateRange) {
			exercises(limit: $limit, range: $range) {
				id
				logId
				activityName
				activityTypeId
				averageHeartRate
				calories
				duration
				activeDuration
				steps
				startTime
				elevationGain
				hasGps
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
	`;

	interface HeartRateZone {
		name: string;
		min: number;
		max: number;
		minutes: number;
	}

	interface ActivityLevel {
		name: string;
		minutes: number;
	}

	interface ExerciseRecord {
		id: string;
		logId: string;
		activityName: string;
		activityTypeId: number;
		averageHeartRate: number | null;
		calories: number;
		duration: number;
		activeDuration: number;
		steps: number | null;
		startTime: string;
		elevationGain: number | null;
		hasGps: boolean;
		heartRateZones: HeartRateZone[];
		activityLevels: ActivityLevel[];
	}

	// Derived stats
	let dayStats = $derived({
		totalActiveMinutes: Math.round(exercises.reduce((sum, e) => sum + e.activeDuration, 0) / 60000),
		totalCalories: exercises.reduce((sum, e) => sum + e.calories, 0),
		totalSteps: exercises.reduce((sum, e) => sum + (e.steps ?? 0), 0),
		exerciseCount: exercises.length,
		avgHeartRate: exercises.filter(e => e.averageHeartRate).length > 0
			? Math.round(exercises.filter(e => e.averageHeartRate).reduce((sum, e) => sum + (e.averageHeartRate ?? 0), 0) / exercises.filter(e => e.averageHeartRate).length)
			: 0
	});

	let percentage = $derived(ACTIVE_MINUTES_GOAL > 0 ? Math.round((dayStats.totalActiveMinutes / ACTIVE_MINUTES_GOAL) * 100) : 0);

	// Activity type breakdown
	let activityBreakdown = $derived.by(() => {
		const breakdown = new Map<string, { count: number; minutes: number; calories: number }>();
		for (const ex of exercises) {
			const current = breakdown.get(ex.activityName) ?? { count: 0, minutes: 0, calories: 0 };
			breakdown.set(ex.activityName, {
				count: current.count + 1,
				minutes: current.minutes + Math.round(ex.activeDuration / 60000),
				calories: current.calories + ex.calories
			});
		}
		return Array.from(breakdown.entries())
			.map(([name, stats]) => ({ name, ...stats }))
			.sort((a, b) => b.minutes - a.minutes);
	});

	// 30-day stats
	let trendStats = $derived({
		totalExercises: dailyTrend.reduce((sum, d) => sum + d.count, 0),
		totalMinutes: dailyTrend.reduce((sum, d) => sum + d.value, 0),
		avgMinutesPerDay: dailyTrend.length > 0
			? Math.round(dailyTrend.reduce((sum, d) => sum + d.value, 0) / dailyTrend.length)
			: 0,
		daysWithExercise: dailyTrend.filter(d => d.count > 0).length
	});

	async function fetchDayData(date: Date) {
		const range = {
			from: toLocalISOString(startOfDay(date)),
			to: toLocalISOString(endOfDay(date))
		};

		const result = await client.query(EXERCISE_QUERY, { limit: 50, range }).toPromise();
		if (result.error) {
			throw new Error(result.error.message);
		}

		exercises = result.data?.exercises ?? [];
	}

	async function fetchTrendData(endDate: Date) {
		const trendData: typeof dailyTrend = [];

		for (let i = 29; i >= 0; i--) {
			const date = subDays(endDate, i);
			const range = {
				from: toLocalISOString(startOfDay(date)),
				to: toLocalISOString(endOfDay(date))
			};

			const result = await client.query(EXERCISE_QUERY, { limit: 50, range }).toPromise();
			const dayExercises: ExerciseRecord[] = result.data?.exercises ?? [];

			const totalMinutes = Math.round(dayExercises.reduce((sum, e) => sum + e.activeDuration, 0) / 60000);
			trendData.push({
				date: format(date, 'yyyy-MM-dd'),
				value: totalMinutes,
				count: dayExercises.length
			});
		}

		dailyTrend = trendData;
	}

	async function fetchAllData() {
		loading = true;
		error = null;

		try {
			const currentDate = $selectedDate;
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

	function toggleExpand(id: string) {
		expandedId = expandedId === id ? null : id;
	}

	function formatTime(dateStr: string): string {
		try {
			return format(parseISO(dateStr), 'h:mm a');
		} catch {
			return dateStr;
		}
	}

	function getZoneColor(zoneName: string): string {
		if (zoneName === 'Out of Range') return heartRateZoneColors['Out of Range'];
		if (zoneName === 'Fat Burn') return heartRateZoneColors['Fat Burn'];
		if (zoneName === 'Cardio') return heartRateZoneColors['Cardio'];
		if (zoneName === 'Peak') return heartRateZoneColors['Peak'];
		return '#666';
	}

	let selectedDateStr = $derived(format($selectedDate, 'yyyy-MM-dd'));

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
	<title>Exercise | Fitbit Dashboard</title>
</svelte:head>

<div class="p-4 sm:p-6 lg:p-8">
	<div class="mb-6">
		<a href="/" class="text-fitbit-active hover:underline text-sm">&larr; Back to Dashboard</a>
	</div>

	<div class="flex items-center gap-3 mb-2">
		<svg class="w-8 h-8 text-fitbit-active" fill="currentColor" viewBox="0 0 24 24">
			<path d="M13.49 5.48c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm-3.6 13.9l1-4.4 2.1 2v6h2v-7.5l-2.1-2 .6-3c1.3 1.5 3.3 2.5 5.5 2.5v-2c-1.9 0-3.5-1-4.3-2.4l-1-1.6c-.4-.6-1-1-1.7-1-.3 0-.5.1-.8.1l-5.2 2.2v4.7h2v-3.4l1.8-.7-1.6 8.1-4.9-1-.4 2 7 1.4z"/>
		</svg>
		<h1 class="text-2xl font-bold text-white">Exercise</h1>
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
		<!-- Day Summary -->
		<div class="bg-dark-card rounded-xl border border-dark-border p-6 mb-6">
			<h2 class="text-lg font-semibold text-white mb-4">
				{format($selectedDate, 'EEEE, MMMM d')}
			</h2>

			<div class="flex flex-col lg:flex-row gap-6">
				<!-- Progress Ring -->
				<div class="flex items-center gap-4">
					<ProgressRing
						value={dayStats.totalActiveMinutes}
						max={ACTIVE_MINUTES_GOAL}
						size={120}
						strokeWidth={10}
						color={colors.active}
					>
						<div class="text-center">
							<p class="text-xl font-bold text-white">{dayStats.totalActiveMinutes}</p>
							<p class="text-xs text-gray-400">minutes</p>
						</div>
					</ProgressRing>
					<div class="space-y-1">
						<p class="text-sm text-gray-400">Goal: {ACTIVE_MINUTES_GOAL} min</p>
						<p class="text-sm {percentage >= 100 ? 'text-green-400' : 'text-gray-500'}">
							{percentage}% {percentage >= 100 ? 'achieved' : 'of goal'}
						</p>
					</div>
				</div>

				<!-- Stats Grid -->
				<div class="flex-1 grid grid-cols-2 md:grid-cols-4 gap-4">
					<div class="text-center p-3 bg-dark-bg rounded-lg">
						<p class="text-xs text-gray-500 uppercase">Activities</p>
						<p class="text-lg font-bold text-white">{dayStats.exerciseCount}</p>
					</div>
					<div class="text-center p-3 bg-dark-bg rounded-lg">
						<p class="text-xs text-gray-500 uppercase">Calories</p>
						<p class="text-lg font-bold text-white">{formatNumber(dayStats.totalCalories)}</p>
					</div>
					<div class="text-center p-3 bg-dark-bg rounded-lg">
						<p class="text-xs text-gray-500 uppercase">Steps</p>
						<p class="text-lg font-bold text-white">{formatNumber(dayStats.totalSteps)}</p>
					</div>
					<div class="text-center p-3 bg-dark-bg rounded-lg">
						<p class="text-xs text-gray-500 uppercase">Avg HR</p>
						<p class="text-lg font-bold text-fitbit-heartrate">{dayStats.avgHeartRate || '--'} <span class="text-xs text-gray-400">bpm</span></p>
					</div>
				</div>
			</div>
		</div>

		<!-- Exercise Log -->
		{#if exercises.length > 0}
			<div class="bg-dark-card rounded-xl border border-dark-border p-6 mb-6">
				<h2 class="text-lg font-semibold text-white mb-4">Activities</h2>

				<div class="space-y-3">
					{#each exercises as exercise}
						{@const isExpanded = expandedId === exercise.id}
						{@const durationMin = Math.round(exercise.activeDuration / 60000)}
						<div class="bg-dark-bg rounded-lg overflow-hidden">
							<!-- Exercise Header -->
							<button
								type="button"
								class="w-full p-4 flex items-center justify-between hover:bg-dark-border/50 transition-colors"
								onclick={() => toggleExpand(exercise.id)}
							>
								<div class="flex items-center gap-3">
									<div class="w-10 h-10 rounded-full bg-fitbit-active/20 flex items-center justify-center">
										<svg class="w-5 h-5 text-fitbit-active" fill="currentColor" viewBox="0 0 24 24">
											<path d="M13.49 5.48c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm-3.6 13.9l1-4.4 2.1 2v6h2v-7.5l-2.1-2 .6-3c1.3 1.5 3.3 2.5 5.5 2.5v-2c-1.9 0-3.5-1-4.3-2.4l-1-1.6c-.4-.6-1-1-1.7-1-.3 0-.5.1-.8.1l-5.2 2.2v4.7h2v-3.4l1.8-.7-1.6 8.1-4.9-1-.4 2 7 1.4z"/>
										</svg>
									</div>
									<div class="text-left">
										<p class="font-medium text-white">{exercise.activityName}</p>
										<p class="text-xs text-gray-500">{formatTime(exercise.startTime)}</p>
									</div>
								</div>
								<div class="flex items-center gap-4">
									<div class="text-right">
										<p class="font-medium text-white">{formatDuration(durationMin)}</p>
										<p class="text-xs text-gray-500">{exercise.calories} cal</p>
									</div>
									<svg
										class="w-5 h-5 text-gray-500 transition-transform {isExpanded ? 'rotate-180' : ''}"
										fill="none"
										stroke="currentColor"
										viewBox="0 0 24 24"
									>
										<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
									</svg>
								</div>
							</button>

							<!-- Expanded Details -->
							{#if isExpanded}
								<div class="px-4 pb-4 border-t border-dark-border">
									<!-- Stats Row -->
									<div class="grid grid-cols-3 gap-4 py-4">
										{#if exercise.steps}
											<div class="text-center">
												<p class="text-xs text-gray-500">Steps</p>
												<p class="font-medium text-white">{formatNumber(exercise.steps)}</p>
											</div>
										{/if}
										{#if exercise.averageHeartRate}
											<div class="text-center">
												<p class="text-xs text-gray-500">Avg Heart Rate</p>
												<p class="font-medium text-fitbit-heartrate">{exercise.averageHeartRate} bpm</p>
											</div>
										{/if}
										{#if exercise.elevationGain}
											<div class="text-center">
												<p class="text-xs text-gray-500">Elevation</p>
												<p class="font-medium text-white">{Math.round(exercise.elevationGain)} ft</p>
											</div>
										{/if}
									</div>

									<!-- Heart Rate Zones -->
									{#if exercise.heartRateZones && exercise.heartRateZones.length > 0}
										<div class="pt-4 border-t border-dark-border">
											<p class="text-sm text-gray-400 mb-3">Heart Rate Zones</p>
											<div class="space-y-2">
												{#each exercise.heartRateZones.filter(z => z.minutes > 0) as zone}
													{@const totalZoneMinutes = exercise.heartRateZones.reduce((s, z) => s + z.minutes, 0)}
													{@const zonePct = totalZoneMinutes > 0 ? (zone.minutes / totalZoneMinutes) * 100 : 0}
													<div class="flex items-center gap-3">
														<div class="w-20 text-xs text-gray-400">{zone.name}</div>
														<div class="flex-1 h-4 bg-dark-border rounded-full overflow-hidden">
															<div
																class="h-full rounded-full"
																style="width: {zonePct}%; background-color: {getZoneColor(zone.name)};"
															></div>
														</div>
														<div class="w-16 text-right text-xs text-gray-400">{zone.minutes} min</div>
													</div>
												{/each}
											</div>
										</div>
									{/if}
								</div>
							{/if}
						</div>
					{/each}
				</div>
			</div>
		{:else}
			<div class="bg-dark-card rounded-xl border border-dark-border p-6 mb-6">
				<p class="text-gray-400 text-center py-8">No activities recorded for this day</p>
			</div>
		{/if}

		<!-- Activity Type Breakdown -->
		{#if activityBreakdown.length > 1}
			<div class="bg-dark-card rounded-xl border border-dark-border p-6 mb-6">
				<h2 class="text-lg font-semibold text-white mb-4">Activity Breakdown</h2>
				<div class="space-y-3">
					{#each activityBreakdown as activity}
						{@const maxMinutes = Math.max(...activityBreakdown.map(a => a.minutes))}
						<div class="flex items-center gap-4">
							<div class="w-32 text-sm text-gray-300 truncate">{activity.name}</div>
							<div class="flex-1 h-6 bg-dark-border rounded-full overflow-hidden">
								<div
									class="h-full rounded-full bg-fitbit-active"
									style="width: {(activity.minutes / maxMinutes) * 100}%;"
								></div>
							</div>
							<div class="w-24 text-right text-sm text-gray-400">
								{activity.minutes} min · {activity.count}x
							</div>
						</div>
					{/each}
				</div>
			</div>
		{/if}

		<!-- 30-Day Trend -->
		<div class="bg-dark-card rounded-xl border border-dark-border p-6">
			<div class="flex justify-between items-center mb-4">
				<h2 class="text-lg font-semibold text-white">Last 30 Days</h2>
				<div class="text-sm text-gray-400">
					Active minutes per day
				</div>
			</div>

			<BarChart
				data={dailyTrend}
				color={colors.active}
				height={200}
				goal={ACTIVE_MINUTES_GOAL}
				formatValue={(v) => `${v} min`}
				onBarClick={handleBarClick}
				selectedDate={selectedDateStr}
			/>

			<!-- Stats Grid -->
			<div class="grid grid-cols-2 md:grid-cols-4 gap-4 mt-6 pt-6 border-t border-dark-border">
				<div>
					<p class="text-xs text-gray-500 uppercase tracking-wide">Total Workouts</p>
					<p class="text-xl font-bold text-white">{trendStats.totalExercises}</p>
				</div>
				<div>
					<p class="text-xs text-gray-500 uppercase tracking-wide">Total Minutes</p>
					<p class="text-xl font-bold text-white">{formatNumber(trendStats.totalMinutes)}</p>
				</div>
				<div>
					<p class="text-xs text-gray-500 uppercase tracking-wide">Daily Average</p>
					<p class="text-xl font-bold text-fitbit-active">{trendStats.avgMinutesPerDay} min</p>
				</div>
				<div>
					<p class="text-xs text-gray-500 uppercase tracking-wide">Active Days</p>
					<p class="text-xl font-bold {trendStats.daysWithExercise > 0 ? 'text-green-400' : 'text-white'}">
						{trendStats.daysWithExercise} / 30
					</p>
				</div>
			</div>
		</div>
	{/if}
</div>
