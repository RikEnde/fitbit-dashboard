<script lang="ts">
    import {onMount} from 'svelte';
    import {gql} from '@urql/svelte';
    import {client} from '$graphql/client';
    import {dateRange} from '$stores/dashboard';
    import {colors, sleepStageColors} from '$utils/colors';
    import {formatDuration} from '$utils/formatters';
    import ProgressRing from '$components/charts/ProgressRing.svelte';

    interface Props {
		goal?: number; // Goal in minutes
	}

	let { goal = 480 }: Props = $props(); // 8 hours default

	// State
	let totalSleep = $state(0); // in minutes
	let sleepStages = $state<{ deep: number; light: number; rem: number; awake: number }>({ deep: 0, light: 0, rem: 0, awake: 0 });
	let efficiency = $state(0);
	let loading = $state(true);
	let error = $state<string | null>(null);

	// Query for sleep data
	const SLEEP_QUERY = gql`
		query Sleeps($limit: Int, $range: DateRange) {
			sleeps(limit: $limit, range: $range) {
				id
				minutesAsleep
				minutesAwake
				efficiency
				levelSummaries {
					level
					minutes
				}
			}
		}
	`;

	interface LevelSummary {
		level: string;
		minutes: number;
	}

	interface SleepRecord {
		id: string;
		minutesAsleep: number;
		minutesAwake: number;
		efficiency: number;
		levelSummaries: LevelSummary[];
	}

	async function fetchData(range: { from: string; to: string }) {
		loading = true;
		error = null;

		try {
			const result = await client.query(SLEEP_QUERY, { limit: 10, range }).toPromise();
			if (result.error) {
				error = result.error.message;
				return;
			}

			const sleepData = result.data?.sleeps ?? [];
			if (sleepData.length > 0) {
				// Take the most recent sleep entry
				const latestSleep = sleepData[0] as SleepRecord;
				totalSleep = latestSleep.minutesAsleep;
				efficiency = latestSleep.efficiency;

				// Process level summaries
				const stages = { deep: 0, light: 0, rem: 0, awake: latestSleep.minutesAwake };
				for (const level of latestSleep.levelSummaries) {
					if (level.level === 'deep') stages.deep = level.minutes;
					else if (level.level === 'light') stages.light = level.minutes;
					else if (level.level === 'rem') stages.rem = level.minutes;
					else if (level.level === 'wake') stages.awake = level.minutes;
				}
				sleepStages = stages;
			} else {
				totalSleep = 0;
				efficiency = 0;
				sleepStages = { deep: 0, light: 0, rem: 0, awake: 0 };
			}
		} catch (e) {
			error = e instanceof Error ? e.message : 'Failed to fetch data';
		} finally {
			loading = false;
		}
	}

	onMount(() => {
		const unsubscribe = dateRange.subscribe((range) => {
			fetchData(range);
		});
		return unsubscribe;
	});

	let percentage = $derived(goal > 0 ? Math.round((totalSleep / goal) * 100) : 0);
	let totalStageMinutes = $derived(sleepStages.deep + sleepStages.light + sleepStages.rem + sleepStages.awake);
</script>

<a
	href="/sleep"
	class="tile block cursor-pointer group"
>
	<div class="flex items-center justify-between mb-3">
		<h3 class="text-sm font-medium text-gray-400 uppercase tracking-wide">Sleep</h3>
		<svg
			class="w-4 h-4 text-gray-500 group-hover:text-white transition-colors"
			fill="none"
			stroke="currentColor"
			viewBox="0 0 24 24"
		>
			<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
		</svg>
	</div>

	{#if loading}
		<div class="animate-pulse space-y-3">
			<div class="h-8 bg-dark-border rounded w-1/2"></div>
			<div class="h-4 bg-dark-border rounded w-3/4"></div>
			<div class="h-10 bg-dark-border rounded w-full mt-3"></div>
		</div>
	{:else if error}
		<div class="text-red-400 text-sm">{error}</div>
	{:else}
		<div class="flex items-center gap-4">
			<!-- Progress Ring -->
			<ProgressRing
				value={totalSleep}
				max={goal}
				size={80}
				strokeWidth={6}
				color={colors.sleep}
			>
				<svg class="w-6 h-6 text-fitbit-sleep" fill="currentColor" viewBox="0 0 24 24">
					<path d="M9.27 4.49c-1.63 7.54 3.75 12.41 7.66 13.8C15.54 19.38 13.81 20 12 20c-4.41 0-8-3.59-8-8 0-3.45 2.2-6.4 5.27-7.51m2.21-2.49C6.1 2.05 2 6.02 2 12c0 5.52 4.48 10 10 10 3.72 0 6.97-2.04 8.69-5.07-6.51-.87-11.15-6.49-9.21-12.93z"/>
				</svg>
			</ProgressRing>

			<!-- Stats -->
			<div class="flex-1">
				{#if totalSleep > 0}
					<p class="text-3xl font-bold text-white">{formatDuration(totalSleep)}</p>
					<p class="text-sm text-gray-400 mt-1">
						Goal: {formatDuration(goal)}
						{#if percentage >= 100}
							<span class="text-green-400 ml-1">({percentage}%)</span>
						{:else}
							<span class="text-gray-500 ml-1">({percentage}%)</span>
						{/if}
					</p>
				{:else}
					<p class="text-xl font-bold text-gray-500">No data</p>
					<p class="text-sm text-gray-500 mt-1">No sleep recorded</p>
				{/if}
			</div>
		</div>

		<!-- Sleep Stages Bar -->
		{#if totalStageMinutes > 0}
			<div class="mt-4">
				<p class="text-xs text-gray-500 mb-2">Sleep Stages</p>
				<div class="flex h-3 rounded-full overflow-hidden">
					<div
						class="transition-all"
						style="width: {(sleepStages.awake / totalStageMinutes) * 100}%; background-color: {sleepStageColors.awake};"
						title="Awake: {sleepStages.awake}m"
					></div>
					<div
						class="transition-all"
						style="width: {(sleepStages.rem / totalStageMinutes) * 100}%; background-color: {sleepStageColors.rem};"
						title="REM: {sleepStages.rem}m"
					></div>
					<div
						class="transition-all"
						style="width: {(sleepStages.light / totalStageMinutes) * 100}%; background-color: {sleepStageColors.light};"
						title="Light: {sleepStages.light}m"
					></div>
					<div
						class="transition-all"
						style="width: {(sleepStages.deep / totalStageMinutes) * 100}%; background-color: {sleepStageColors.deep};"
						title="Deep: {sleepStages.deep}m"
					></div>
				</div>
				<div class="flex justify-between mt-2 text-xs text-gray-500">
					<span style="color: {sleepStageColors.deep}">Deep {sleepStages.deep}m</span>
					<span style="color: {sleepStageColors.light}">Light {sleepStages.light}m</span>
					<span style="color: {sleepStageColors.rem}">REM {sleepStages.rem}m</span>
				</div>
			</div>
		{/if}

		<!-- Efficiency -->
		{#if efficiency > 0}
			<div class="mt-3 pt-3 border-t border-dark-border flex justify-between items-center">
				<span class="text-xs text-gray-500">Sleep Efficiency</span>
				<span class="text-sm font-medium" class:text-green-400={efficiency >= 85} class:text-yellow-400={efficiency >= 70 && efficiency < 85} class:text-red-400={efficiency < 70}>{efficiency}%</span>
			</div>
		{/if}
	{/if}
</a>

<style>
	.tile {
		@apply bg-dark-card rounded-xl p-4 border border-dark-border transition-all duration-200;
	}

	.tile:hover {
		@apply border-gray-500 shadow-lg;
	}
</style>
