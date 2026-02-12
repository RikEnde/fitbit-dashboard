<script lang="ts">
    import {onMount} from 'svelte';
    import {gql} from '@urql/svelte';
    import {client} from '$graphql/client';
    import {dateRange} from '$stores/dashboard';
    import {colors} from '$utils/colors';
    import {formatDuration} from '$utils/formatters';
    import ProgressRing from '$components/charts/ProgressRing.svelte';

    interface Props {
		goal?: number; // Goal in minutes
	}

	let { goal = 30 }: Props = $props();

	// State
	let totalActiveMinutes = $state(0);
	let exerciseCount = $state(0);
	let totalCaloriesBurned = $state(0);
	let loading = $state(true);
	let error = $state<string | null>(null);
	let recentActivities = $state<{ name: string; duration: number; calories: number }[]>([]);

	// Query for exercise data
	const EXERCISE_QUERY = gql`
		query Exercises($limit: Int, $range: DateRange) {
			exercises(limit: $limit, range: $range) {
				id
				activityName
				activeDuration
				calories
				averageHeartRate
			}
		}
	`;

	interface ExerciseRecord {
		id: string;
		activityName: string;
		activeDuration: number; // in milliseconds
		calories: number;
		averageHeartRate: number | null;
	}

	async function fetchData(range: { from: string; to: string }) {
		loading = true;
		error = null;

		try {
			const result = await client.query(EXERCISE_QUERY, { limit: 50, range }).toPromise();
			if (result.error) {
				error = result.error.message;
				return;
			}

			const exercises = result.data?.exercises ?? [];
			exerciseCount = exercises.length;

			// Calculate totals (activeDuration is in milliseconds)
			totalActiveMinutes = Math.round(exercises.reduce((sum: number, e: ExerciseRecord) => sum + e.activeDuration, 0) / 60000);
			totalCaloriesBurned = exercises.reduce((sum: number, e: ExerciseRecord) => sum + e.calories, 0);

			// Get recent activities (top 3)
			recentActivities = exercises.slice(0, 3).map((e: ExerciseRecord) => ({
				name: e.activityName,
				duration: Math.round(e.activeDuration / 60000),
				calories: e.calories
			}));
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

	let percentage = $derived(goal > 0 ? Math.round((totalActiveMinutes / goal) * 100) : 0);
</script>

<a
	href="/exercise"
	class="tile block cursor-pointer group"
>
	<div class="flex items-center justify-between mb-3">
		<h3 class="text-sm font-medium text-theme-text-secondary uppercase tracking-wide">Active Minutes</h3>
		<svg
			class="w-4 h-4 text-theme-text-muted group-hover:text-theme-text transition-colors"
			fill="none"
			stroke="currentColor"
			viewBox="0 0 24 24"
		>
			<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
		</svg>
	</div>

	{#if loading}
		<div class="animate-pulse space-y-3">
			<div class="h-8 bg-theme-border rounded w-1/2"></div>
			<div class="h-4 bg-theme-border rounded w-3/4"></div>
			<div class="h-10 bg-theme-border rounded w-full mt-3"></div>
		</div>
	{:else if error}
		<div class="text-red-400 text-sm">{error}</div>
	{:else}
		<div class="flex items-center gap-4">
			<!-- Progress Ring -->
			<ProgressRing
				value={totalActiveMinutes}
				max={goal}
				size={80}
				strokeWidth={6}
				color={colors.active}
			>
				<svg class="w-6 h-6 text-fitbit-active" fill="currentColor" viewBox="0 0 24 24">
					<path d="M13.49 5.48c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm-3.6 13.9l1-4.4 2.1 2v6h2v-7.5l-2.1-2 .6-3c1.3 1.5 3.3 2.5 5.5 2.5v-2c-1.9 0-3.5-1-4.3-2.4l-1-1.6c-.4-.6-1-1-1.7-1-.3 0-.5.1-.8.1l-5.2 2.2v4.7h2v-3.4l1.8-.7-1.6 8.1-4.9-1-.4 2 7 1.4z"/>
				</svg>
			</ProgressRing>

			<!-- Stats -->
			<div class="flex-1">
				<p class="text-3xl font-bold text-theme-text">{formatDuration(totalActiveMinutes)}</p>
				<p class="text-sm text-theme-text-secondary mt-1">
					Goal: {formatDuration(goal)}
					{#if percentage >= 100}
						<span class="text-green-400 ml-1">({percentage}%)</span>
					{:else}
						<span class="text-theme-text-muted ml-1">({percentage}%)</span>
					{/if}
				</p>
			</div>
		</div>

		<!-- Activity Summary -->
		{#if exerciseCount > 0}
			<div class="mt-4 pt-3 border-t border-theme-border">
				<div class="flex justify-between text-sm mb-2">
					<span class="text-theme-text-muted">Activities</span>
					<span class="text-theme-text font-medium">{exerciseCount}</span>
				</div>
				<div class="flex justify-between text-sm">
					<span class="text-theme-text-muted">Calories burned</span>
					<span class="text-theme-text font-medium">{totalCaloriesBurned.toLocaleString()}</span>
				</div>
			</div>

			<!-- Recent Activities -->
			{#if recentActivities.length > 0}
				<div class="mt-3 space-y-2">
					{#each recentActivities as activity}
						<div class="flex justify-between items-center text-xs">
							<span class="text-theme-text-secondary truncate flex-1">{activity.name}</span>
							<span class="text-theme-text-muted ml-2">{formatDuration(activity.duration)}</span>
						</div>
					{/each}
				</div>
			{/if}
		{:else}
			<div class="mt-4 pt-3 border-t border-theme-border">
				<p class="text-sm text-theme-text-muted">No activities recorded today</p>
			</div>
		{/if}
	{/if}
</a>

<style>
	.tile {
		@apply bg-theme-card rounded-xl p-4 border border-theme-border transition-all duration-200;
	}

	.tile:hover {
		@apply border-gray-500 shadow-lg;
	}
</style>
