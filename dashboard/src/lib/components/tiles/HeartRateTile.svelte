<script lang="ts">
    import {onMount} from 'svelte';
    import {gql} from '@urql/svelte';
    import {client} from '$graphql/client';
    import {dateRange} from '$stores/dashboard';
    import {colors, heartRateZoneColors} from '$utils/colors';
    import MiniBarChart from '$components/charts/MiniBarChart.svelte';

    // State
	let currentBpm = $state(0);
	let minBpm = $state(0);
	let maxBpm = $state(0);
	let avgBpm = $state(0);
	let loading = $state(true);
	let error = $state<string | null>(null);
	let hourlyData = $state<{ label: string; value: number }[]>([]);

	// Query for heart rate data
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
				label: `${hour}:00`,
				value: count > 0 ? Math.round(sum / count) : 0
			}));
	}

	function getHeartRateZone(bpm: number): { name: string; color: string } {
		if (bpm < 60) return { name: 'Resting', color: heartRateZoneColors['Out of Range'] };
		if (bpm < 100) return { name: 'Out of Range', color: heartRateZoneColors['Out of Range'] };
		if (bpm < 140) return { name: 'Fat Burn', color: heartRateZoneColors['Fat Burn'] };
		if (bpm < 170) return { name: 'Cardio', color: heartRateZoneColors['Cardio'] };
		return { name: 'Peak', color: heartRateZoneColors['Peak'] };
	}

	async function fetchData(range: { from: string; to: string }) {
		loading = true;
		error = null;

		try {
			const result = await client.query(HEART_RATE_QUERY, { limit: 10000, range }).toPromise();
			if (result.error) {
				error = result.error.message;
				return;
			}

			const heartRates = result.data?.heartRates ?? [];
			if (heartRates.length > 0) {
				const bpms = heartRates.map((hr: HeartRateRecord) => hr.bpm);
				currentBpm = heartRates[heartRates.length - 1].bpm;
				minBpm = Math.min(...bpms);
				maxBpm = Math.max(...bpms);
				avgBpm = Math.round(bpms.reduce((a: number, b: number) => a + b, 0) / bpms.length);
				hourlyData = processHourlyData(heartRates);
			} else {
				currentBpm = 0;
				minBpm = 0;
				maxBpm = 0;
				avgBpm = 0;
				hourlyData = [];
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

	let zone = $derived(getHeartRateZone(currentBpm));
</script>

<a
	href="/heartrate"
	class="tile block cursor-pointer group"
>
	<div class="flex items-center justify-between mb-3">
		<h3 class="text-sm font-medium text-gray-400 uppercase tracking-wide">Heart Rate</h3>
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
			<!-- Heart Icon with Pulse -->
			<div class="relative flex items-center justify-center w-20 h-20">
				<svg class="w-12 h-12" fill={colors.heartrate} viewBox="0 0 24 24">
					<path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/>
				</svg>
				{#if currentBpm > 0}
					<div class="absolute inset-0 flex items-center justify-center animate-pulse">
						<svg class="w-14 h-14 opacity-30" fill={colors.heartrate} viewBox="0 0 24 24">
							<path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/>
						</svg>
					</div>
				{/if}
			</div>

			<!-- Stats -->
			<div class="flex-1">
				{#if currentBpm > 0}
					<p class="text-3xl font-bold text-white">{currentBpm} <span class="text-lg text-gray-400">bpm</span></p>
					<p class="text-sm mt-1" style="color: {zone.color}">{zone.name}</p>
				{:else}
					<p class="text-xl font-bold text-gray-500">No data</p>
				{/if}
			</div>
		</div>

		<!-- Stats Row -->
		{#if avgBpm > 0}
			<div class="flex justify-between mt-4 text-sm">
				<div class="text-center">
					<p class="text-gray-500">Min</p>
					<p class="text-white font-medium">{minBpm}</p>
				</div>
				<div class="text-center">
					<p class="text-gray-500">Avg</p>
					<p class="text-white font-medium">{avgBpm}</p>
				</div>
				<div class="text-center">
					<p class="text-gray-500">Max</p>
					<p class="text-white font-medium">{maxBpm}</p>
				</div>
			</div>
		{/if}

		<!-- Mini Hourly Chart -->
		<div class="mt-4">
			<p class="text-xs text-gray-500 mb-2">Today's Heart Rate</p>
			<MiniBarChart data={hourlyData} color={colors.heartrate} height={32} />
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
