<script lang="ts">
    import {onMount} from 'svelte';
    import {gql} from '@urql/svelte';
    import {client} from '$graphql/client';
    import {formattedDate, selectedDate, setDate} from '$stores/dashboard';
    import {colors, sleepStageColors} from '$utils/colors';
    import {endOfDay, format, parseISO, startOfDay, subDays} from 'date-fns';
    import ProgressRing from '$components/charts/ProgressRing.svelte';
    import BarChart from '$components/charts/BarChart.svelte';
    import SleepStagesChart from '$components/charts/SleepStagesChart.svelte';

    // Constants
	const SLEEP_GOAL = 480; // 8 hours in minutes

	// State
	let loading = $state(true);
	let error = $state<string | null>(null);

	// Current night's sleep data
	let sleepLog = $state<SleepRecord | null>(null);
	let sleepScore = $state<SleepScoreRecord | null>(null);

	// 30-day trend
	let dailyTrend = $state<{ date: string; value: number; efficiency: number }[]>([]);

	// GraphQL Queries
	const SLEEP_QUERY = gql`
		query Sleeps($limit: Int, $range: DateRange) {
			sleeps(limit: $limit, range: $range) {
				id
				logId
				dateOfSleep
				startTime
				endTime
				duration
				minutesToFallAsleep
				minutesAsleep
				minutesAwake
				minutesAfterWakeup
				timeInBed
				efficiency
				mainSleep
				levelSummaries {
					level
					count
					minutes
					thirtyDayAvgMinutes
				}
				levelData {
					dateTime
					level
					seconds
				}
			}
		}
	`;

	const SLEEP_SCORE_QUERY = gql`
		query SleepScores($limit: Int, $range: DateRange) {
			sleepScores(limit: $limit, range: $range) {
				id
				sleepLogEntryId
				timestamp
				overallScore
				compositionScore
				revitalizationScore
				durationScore
				deepSleepInMinutes
				restingHeartRate
				restlessness
			}
		}
	`;

	interface LevelSummary {
		level: string;
		count: number;
		minutes: number;
		thirtyDayAvgMinutes: number;
	}

	interface LevelData {
		dateTime: string;
		level: string;
		seconds: number;
	}

	interface SleepRecord {
		id: string;
		logId: string;
		dateOfSleep: string;
		startTime: string;
		endTime: string;
		duration: number;
		minutesToFallAsleep: number;
		minutesAsleep: number;
		minutesAwake: number;
		minutesAfterWakeup: number;
		timeInBed: number;
		efficiency: number;
		mainSleep: boolean;
		levelSummaries: LevelSummary[];
		levelData: LevelData[];
	}

	interface SleepScoreRecord {
		id: string;
		sleepLogEntryId: string;
		timestamp: string;
		overallScore: number;
		compositionScore: number | null;
		revitalizationScore: number;
		durationScore: number | null;
		deepSleepInMinutes: number;
		restingHeartRate: number;
		restlessness: number;
	}

	// Derived values
	let sleepStages = $derived.by(() => {
		if (!sleepLog) return { deep: 0, light: 0, rem: 0, awake: 0 };

		const stages = { deep: 0, light: 0, rem: 0, awake: 0 };
		for (const level of sleepLog.levelSummaries) {
			if (level.level === 'deep') stages.deep = level.minutes;
			else if (level.level === 'light') stages.light = level.minutes;
			else if (level.level === 'rem') stages.rem = level.minutes;
			else if (level.level === 'wake') stages.awake = level.minutes;
		}
		return stages;
	});

	let totalStageMinutes = $derived(
		sleepStages.deep + sleepStages.light + sleepStages.rem + sleepStages.awake
	);

	let percentage = $derived(
		sleepLog && SLEEP_GOAL > 0 ? Math.round((sleepLog.minutesAsleep / SLEEP_GOAL) * 100) : 0
	);

	// 30-day stats
	let trendStats = $derived({
		avgSleep: dailyTrend.length > 0
			? Math.round(dailyTrend.filter(d => d.value > 0).reduce((sum, d) => sum + d.value, 0) / dailyTrend.filter(d => d.value > 0).length)
			: 0,
		avgEfficiency: dailyTrend.length > 0
			? Math.round(dailyTrend.filter(d => d.efficiency > 0).reduce((sum, d) => sum + d.efficiency, 0) / dailyTrend.filter(d => d.efficiency > 0).length)
			: 0,
		bestSleep: dailyTrend.length > 0
			? Math.max(...dailyTrend.map(d => d.value))
			: 0,
		daysMetGoal: dailyTrend.filter(d => d.value >= SLEEP_GOAL).length
	});

	async function fetchDayData(date: Date) {
		// Sleep logs are typically stored for the night ending on a given day
		// So we look at the full day range
		const range = {
			from: startOfDay(subDays(date, 1)).toISOString(), // Include previous night
			to: endOfDay(date).toISOString()
		};

		const [sleepResult, scoreResult] = await Promise.all([
			client.query(SLEEP_QUERY, { limit: 5, range }).toPromise(),
			client.query(SLEEP_SCORE_QUERY, { limit: 5, range }).toPromise()
		]);

		if (sleepResult.error) {
			throw new Error(sleepResult.error.message);
		}

		const sleeps: SleepRecord[] = sleepResult.data?.sleeps ?? [];
		// Find the main sleep log for this date
		const mainSleep = sleeps.find(s => s.mainSleep) ?? sleeps[0] ?? null;
		sleepLog = mainSleep;

		// Match sleep score to sleep log
		const scores: SleepScoreRecord[] = scoreResult.data?.sleepScores ?? [];
		if (mainSleep && scores.length > 0) {
			sleepScore = scores.find(s => s.sleepLogEntryId === mainSleep.logId) ?? scores[0] ?? null;
		} else {
			sleepScore = null;
		}
	}

	async function fetchTrendData(endDate: Date) {
		const trendData: typeof dailyTrend = [];

		// Fetch 30 days of sleep data
		for (let i = 29; i >= 0; i--) {
			const date = subDays(endDate, i);
			const range = {
				from: startOfDay(subDays(date, 1)).toISOString(),
				to: endOfDay(date).toISOString()
			};

			const result = await client.query(SLEEP_QUERY, { limit: 5, range }).toPromise();
			const sleeps: SleepRecord[] = result.data?.sleeps ?? [];
			const mainSleep = sleeps.find(s => s.mainSleep) ?? sleeps[0];

			if (mainSleep) {
				trendData.push({
					date: format(date, 'yyyy-MM-dd'),
					value: mainSleep.minutesAsleep,
					efficiency: mainSleep.efficiency
				});
			} else {
				trendData.push({
					date: format(date, 'yyyy-MM-dd'),
					value: 0,
					efficiency: 0
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

	function formatSleepDuration(minutes: number): string {
		const hours = Math.floor(minutes / 60);
		const mins = minutes % 60;
		return `${hours}h ${mins}m`;
	}

	function getScoreColor(score: number): string {
		if (score >= 80) return 'text-green-400';
		if (score >= 60) return 'text-yellow-400';
		return 'text-red-400';
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
	<title>Sleep | Fitbit Dashboard</title>
</svelte:head>

<div class="p-4 sm:p-6 lg:p-8">
	<div class="mb-6">
		<a href="/" class="text-fitbit-sleep hover:underline text-sm">&larr; Back to Dashboard</a>
	</div>

	<div class="flex items-center gap-3 mb-2">
		<svg class="w-8 h-8 text-fitbit-sleep" fill="currentColor" viewBox="0 0 24 24">
			<path d="M9.27 4.49c-1.63 7.54 3.75 12.41 7.66 13.8C15.54 19.38 13.81 20 12 20c-4.41 0-8-3.59-8-8 0-3.45 2.2-6.4 5.27-7.51m2.21-2.49C6.1 2.05 2 6.02 2 12c0 5.52 4.48 10 10 10 3.72 0 6.97-2.04 8.69-5.07-6.51-.87-11.15-6.49-9.21-12.93z"/>
		</svg>
		<h1 class="text-2xl font-bold text-white">Sleep</h1>
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
	{:else if !sleepLog}
		<div class="bg-dark-card rounded-xl border border-dark-border p-6">
			<p class="text-gray-400 text-center py-12">No sleep data recorded for this date</p>
		</div>
	{:else}
		<!-- Sleep Summary Card -->
		<div class="bg-dark-card rounded-xl border border-dark-border p-6 mb-6">
			<div class="flex justify-between items-start mb-4">
				<h2 class="text-lg font-semibold text-white">
					{format(parseISO(sleepLog.dateOfSleep), 'EEEE, MMMM d')}
				</h2>
				{#if sleepScore}
					<div class="text-right">
						<p class="text-xs text-gray-500 uppercase">Sleep Score</p>
						<p class="text-3xl font-bold {getScoreColor(sleepScore.overallScore)}">{sleepScore.overallScore}</p>
					</div>
				{/if}
			</div>

			<div class="flex flex-col lg:flex-row gap-6">
				<!-- Duration & Progress -->
				<div class="flex items-center gap-4">
					<ProgressRing
						value={sleepLog.minutesAsleep}
						max={SLEEP_GOAL}
						size={120}
						strokeWidth={10}
						color={colors.sleep}
					>
						<div class="text-center">
							<p class="text-xl font-bold text-white">{formatSleepDuration(sleepLog.minutesAsleep)}</p>
							<p class="text-xs text-gray-400">asleep</p>
						</div>
					</ProgressRing>
					<div class="space-y-1">
						<p class="text-sm text-gray-400">Goal: {formatSleepDuration(SLEEP_GOAL)}</p>
						<p class="text-sm {percentage >= 100 ? 'text-green-400' : 'text-gray-500'}">
							{percentage}% {percentage >= 100 ? 'achieved' : 'of goal'}
						</p>
						<p class="text-xs text-gray-500 mt-2">
							{format(parseISO(sleepLog.startTime), 'h:mm a')} - {format(parseISO(sleepLog.endTime), 'h:mm a')}
						</p>
					</div>
				</div>

				<!-- Stats Grid -->
				<div class="flex-1 grid grid-cols-2 md:grid-cols-4 gap-4">
					<div class="text-center p-3 bg-dark-bg rounded-lg">
						<p class="text-xs text-gray-500 uppercase">Time in Bed</p>
						<p class="text-lg font-bold text-white">{formatSleepDuration(sleepLog.timeInBed)}</p>
					</div>
					<div class="text-center p-3 bg-dark-bg rounded-lg">
						<p class="text-xs text-gray-500 uppercase">Time Awake</p>
						<p class="text-lg font-bold text-white">{formatSleepDuration(sleepLog.minutesAwake)}</p>
					</div>
					<div class="text-center p-3 bg-dark-bg rounded-lg">
						<p class="text-xs text-gray-500 uppercase">Fell Asleep In</p>
						<p class="text-lg font-bold text-white">{sleepLog.minutesToFallAsleep}m</p>
					</div>
					<div class="text-center p-3 bg-dark-bg rounded-lg">
						<p class="text-xs text-gray-500 uppercase">Efficiency</p>
						<p class="text-lg font-bold {sleepLog.efficiency >= 85 ? 'text-green-400' : sleepLog.efficiency >= 70 ? 'text-yellow-400' : 'text-red-400'}">
							{sleepLog.efficiency}%
						</p>
					</div>
				</div>
			</div>
		</div>

		<!-- Sleep Stages Timeline -->
		{#if sleepLog.levelData && sleepLog.levelData.length > 0}
			<div class="bg-dark-card rounded-xl border border-dark-border p-6 mb-6">
				<h2 class="text-lg font-semibold text-white mb-4">Sleep Stages</h2>
				<SleepStagesChart
					data={sleepLog.levelData}
					startTime={sleepLog.startTime}
					endTime={sleepLog.endTime}
					height={140}
				/>

				<!-- Stage Summary -->
				<div class="grid grid-cols-4 gap-4 mt-6 pt-4 border-t border-dark-border">
					<div class="text-center">
						<div class="flex items-center justify-center gap-2 mb-1">
							<div class="w-3 h-3 rounded" style="background-color: {sleepStageColors.awake}"></div>
							<span class="text-xs text-gray-400">Awake</span>
						</div>
						<p class="text-lg font-bold text-white">{sleepStages.awake}m</p>
						<p class="text-xs text-gray-500">{totalStageMinutes > 0 ? Math.round((sleepStages.awake / totalStageMinutes) * 100) : 0}%</p>
					</div>
					<div class="text-center">
						<div class="flex items-center justify-center gap-2 mb-1">
							<div class="w-3 h-3 rounded" style="background-color: {sleepStageColors.rem}"></div>
							<span class="text-xs text-gray-400">REM</span>
						</div>
						<p class="text-lg font-bold text-white">{sleepStages.rem}m</p>
						<p class="text-xs text-gray-500">{totalStageMinutes > 0 ? Math.round((sleepStages.rem / totalStageMinutes) * 100) : 0}%</p>
					</div>
					<div class="text-center">
						<div class="flex items-center justify-center gap-2 mb-1">
							<div class="w-3 h-3 rounded" style="background-color: {sleepStageColors.light}"></div>
							<span class="text-xs text-gray-400">Light</span>
						</div>
						<p class="text-lg font-bold text-white">{sleepStages.light}m</p>
						<p class="text-xs text-gray-500">{totalStageMinutes > 0 ? Math.round((sleepStages.light / totalStageMinutes) * 100) : 0}%</p>
					</div>
					<div class="text-center">
						<div class="flex items-center justify-center gap-2 mb-1">
							<div class="w-3 h-3 rounded" style="background-color: {sleepStageColors.deep}"></div>
							<span class="text-xs text-gray-400">Deep</span>
						</div>
						<p class="text-lg font-bold text-white">{sleepStages.deep}m</p>
						<p class="text-xs text-gray-500">{totalStageMinutes > 0 ? Math.round((sleepStages.deep / totalStageMinutes) * 100) : 0}%</p>
					</div>
				</div>
			</div>
		{/if}

		<!-- Sleep Score Breakdown -->
		{#if sleepScore}
			<div class="bg-dark-card rounded-xl border border-dark-border p-6 mb-6">
				<h2 class="text-lg font-semibold text-white mb-4">Sleep Score Breakdown</h2>
				<div class="grid grid-cols-2 md:grid-cols-4 gap-4">
					<div class="text-center p-4 bg-dark-bg rounded-lg">
						<p class="text-xs text-gray-500 uppercase mb-2">Duration</p>
						<p class="text-2xl font-bold {getScoreColor(sleepScore.durationScore ?? 0)}">{sleepScore.durationScore ?? '--'}</p>
					</div>
					<div class="text-center p-4 bg-dark-bg rounded-lg">
						<p class="text-xs text-gray-500 uppercase mb-2">Composition</p>
						<p class="text-2xl font-bold {getScoreColor(sleepScore.compositionScore ?? 0)}">{sleepScore.compositionScore ?? '--'}</p>
					</div>
					<div class="text-center p-4 bg-dark-bg rounded-lg">
						<p class="text-xs text-gray-500 uppercase mb-2">Revitalization</p>
						<p class="text-2xl font-bold {getScoreColor(sleepScore.revitalizationScore)}">{sleepScore.revitalizationScore}</p>
					</div>
					<div class="text-center p-4 bg-dark-bg rounded-lg">
						<p class="text-xs text-gray-500 uppercase mb-2">Resting HR</p>
						<p class="text-2xl font-bold text-fitbit-heartrate">{sleepScore.restingHeartRate} <span class="text-sm text-gray-400">bpm</span></p>
					</div>
				</div>
			</div>
		{/if}

		<!-- 30-Day Trend -->
		<div class="bg-dark-card rounded-xl border border-dark-border p-6">
			<div class="flex justify-between items-center mb-4">
				<h2 class="text-lg font-semibold text-white">Last 30 Days</h2>
				<div class="text-sm text-gray-400">
					Sleep duration trend
				</div>
			</div>

			<BarChart
				data={dailyTrend}
				color={colors.sleep}
				height={200}
				goal={SLEEP_GOAL}
				formatValue={formatSleepDuration}
				onBarClick={handleBarClick}
				selectedDate={selectedDateStr}
			/>

			<!-- Stats Grid -->
			<div class="grid grid-cols-2 md:grid-cols-4 gap-4 mt-6 pt-6 border-t border-dark-border">
				<div>
					<p class="text-xs text-gray-500 uppercase tracking-wide">Avg Sleep</p>
					<p class="text-xl font-bold text-white">{formatSleepDuration(trendStats.avgSleep)}</p>
				</div>
				<div>
					<p class="text-xs text-gray-500 uppercase tracking-wide">Avg Efficiency</p>
					<p class="text-xl font-bold {trendStats.avgEfficiency >= 85 ? 'text-green-400' : 'text-white'}">{trendStats.avgEfficiency}%</p>
				</div>
				<div>
					<p class="text-xs text-gray-500 uppercase tracking-wide">Best Night</p>
					<p class="text-xl font-bold text-fitbit-sleep">{formatSleepDuration(trendStats.bestSleep)}</p>
				</div>
				<div>
					<p class="text-xs text-gray-500 uppercase tracking-wide">Met Goal</p>
					<p class="text-xl font-bold {trendStats.daysMetGoal > 0 ? 'text-green-400' : 'text-white'}">
						{trendStats.daysMetGoal} / 30
					</p>
				</div>
			</div>
		</div>
	{/if}
</div>
