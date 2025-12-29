<script lang="ts">
    import {onMount} from 'svelte';
    import {gql} from '@urql/svelte';
    import {client} from '$graphql/client';
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

	// Query for distance data
	const DISTANCE_QUERY = gql`
		query Distances($limit: Int, $range: DateRange) {
			distances(limit: $limit, range: $range) {
				id
				value
				dateTime
			}
		}
	`;

	interface DistanceRecord {
		id: string;
		value: number;
		dateTime: string;
	}

	function processHourlyData(distances: DistanceRecord[]): { label: string; value: number }[] {
		const hourlyMap = new Map<number, number>();
		for (let i = 0; i < 24; i++) {
			hourlyMap.set(i, 0);
		}
		for (const dist of distances) {
			const hour = new Date(dist.dateTime).getHours();
			hourlyMap.set(hour, (hourlyMap.get(hour) ?? 0) + dist.value);
		}
		return Array.from(hourlyMap.entries())
			.sort((a, b) => a[0] - b[0])
			.map(([hour, value]) => ({
				label: `${hour}:00`,
				value: value / CM_TO_KM // Convert cm to km for display
			}));
	}

	async function fetchData(range: { from: string; to: string }) {
		loading = true;
		error = null;

		try {
			const result = await client.query(DISTANCE_QUERY, { limit: 1440, range }).toPromise();
			if (result.error) {
				error = result.error.message;
				return;
			}

			const distanceData = result.data?.distances ?? [];
			const totalCm = distanceData.reduce((sum: number, d: DistanceRecord) => sum + d.value, 0);
			totalDistanceKm = totalCm / CM_TO_KM; // Convert cm to km
			hourlyData = processHourlyData(distanceData);
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
		<h3 class="text-sm font-medium text-gray-400 uppercase tracking-wide">Distance</h3>
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
				<p class="text-3xl font-bold text-white">{displayDistance} <span class="text-lg text-gray-400">km</span></p>
				<p class="text-sm text-gray-400 mt-1">
					Goal: {goal} km
					{#if percentage >= 100}
						<span class="text-green-400 ml-1">({percentage}%)</span>
					{:else}
						<span class="text-gray-500 ml-1">({percentage}%)</span>
					{/if}
				</p>
			</div>
		</div>

		<!-- Mini Hourly Chart -->
		<div class="mt-4">
			<p class="text-xs text-gray-500 mb-2">Today's Journey</p>
			<MiniBarChart data={hourlyData} color={colors.distance} height={32} />
		</div>
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
