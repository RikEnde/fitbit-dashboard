<script lang="ts">
    import {onMount} from 'svelte';
    import {client} from '$graphql/client';
    import {DAILY_DISTANCE_SUM_QUERY, DISTANCE_PER_INTERVAL_QUERY} from '$graphql/queries';
    import {dateRange} from '$stores/dashboard';
    import {colors} from '$utils/colors';
    import ProgressRing from '$components/charts/ProgressRing.svelte';
    import MiniBarChart from '$components/charts/MiniBarChart.svelte';

    // Distance values from Fitbit are stored in centimeters
	const CM_TO_KM = 100000;

	interface Props {
		goal?: number; // Goal in km
	}

	let { goal = 8 }: Props = $props();

	// State
	let totalDistanceKm = $state(0);
	let loading = $state(true);
	let error = $state<string | null>(null);
	let hourlyData = $state<{ label: string; value: number }[]>([]);

	interface IntervalRecord {
		timeInterval: string;
		sum: number;
	}

	function processIntervalData(intervals: IntervalRecord[]): { label: string; value: number }[] {
		return intervals.map((interval) => {
			const hour = new Date(interval.timeInterval).getHours();
			return {
				label: `${hour}:00`,
				value: interval.sum / CM_TO_KM // Convert cm to km for display
			};
		});
	}

	async function fetchData(range: { from: string; to: string }) {
		loading = true;
		error = null;

		try {
			const [dailyResult, hourlyResult] = await Promise.all([
				client.query(DAILY_DISTANCE_SUM_QUERY, { range }).toPromise(),
				client.query(DISTANCE_PER_INTERVAL_QUERY, { range, duration: '1 hour' }).toPromise()
			]);

			if (dailyResult.error) {
				error = dailyResult.error.message;
				return;
			}

			const totalCm = dailyResult.data?.dailyDistanceSum?.[0]?.totalDistance ?? 0;
			totalDistanceKm = totalCm / CM_TO_KM;

			if (hourlyResult.data?.distancePerInterval) {
				hourlyData = processIntervalData(hourlyResult.data.distancePerInterval);
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

	let percentage = $derived(goal > 0 ? Math.round((totalDistanceKm / goal) * 100) : 0);
	let displayDistance = $derived(totalDistanceKm.toFixed(2));
</script>

<a
	href="/distance"
	class="tile block cursor-pointer group"
>
	<div class="flex items-center justify-between mb-3">
		<h3 class="text-sm font-medium text-theme-text-secondary uppercase tracking-wide">Distance</h3>
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
				value={totalDistanceKm}
				max={goal}
				size={80}
				strokeWidth={6}
				color={colors.distance}
			>
				<svg class="w-6 h-6 text-fitbit-distance" fill="currentColor" viewBox="0 0 24 24">
					<path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5c-1.38 0-2.5-1.12-2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 2.5-2.5 2.5z"/>
				</svg>
			</ProgressRing>

			<!-- Stats -->
			<div class="flex-1">
				<p class="text-3xl font-bold text-theme-text">{displayDistance} <span class="text-lg text-theme-text-secondary">km</span></p>
				<p class="text-sm text-theme-text-secondary mt-1">
					Goal: {goal} km
					{#if percentage >= 100}
						<span class="text-green-400 ml-1">({percentage}%)</span>
					{:else}
						<span class="text-theme-text-muted ml-1">({percentage}%)</span>
					{/if}
				</p>
			</div>
		</div>

		<!-- Mini Hourly Chart -->
		<div class="mt-4">
			<p class="text-xs text-theme-text-muted mb-2">Today's Journey</p>
			<MiniBarChart data={hourlyData} color={colors.distance} height={32} />
		</div>
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
