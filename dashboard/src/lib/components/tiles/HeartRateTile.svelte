<script lang="ts">
    import {onMount} from 'svelte';
    import {client} from '$graphql/client';
    import {HEART_RATE_QUERY} from '$graphql/queries';
    import {dateRange} from '$stores/dashboard';
    import {colors} from '$utils/colors';
    import MiniBarChart from '$components/charts/MiniBarChart.svelte';

    // State
	let minBpm = $state(0);
	let maxBpm = $state(0);
	let restingBpm = $state(0);
	let loading = $state(true);
	let error = $state<string | null>(null);
	let hourlyData = $state<{ label: string; value: number }[]>([]);

	interface HeartRateRecord {
		id: string;
		bpm: number;
		dateTime: string;
	}

	function processHourlyData(heartRates: HeartRateRecord[]): { label: string; value: number }[] {
		const hourlyMap = new Map<number, { sum: number; count: number }>();
		for (let i = 0; i < 24; i++) {
			hourlyMap.set(i, { sum: 0, count: 0 });
		}
		for (const hr of heartRates) {
			const hour = new Date(hr.dateTime).getHours();
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

	async function fetchData(range: { from: string; to: string }) {
		loading = true;
		error = null;

		try {
			// Extract date from range for resting heart rate query (YYYY-MM-DD format)
			const date = range.to.split('T')[0];
			const result = await client.query(HEART_RATE_QUERY, { limit: 10000, range, date }).toPromise();
			if (result.error) {
				error = result.error.message;
				return;
			}

			const heartRates = result.data?.heartRates ?? [];
			const restingHr = result.data?.restingHeartRate;
			restingBpm = restingHr ? Math.round(restingHr.value) : 0;
			if (heartRates.length > 0) {
				const bpms = heartRates.map((hr: HeartRateRecord) => hr.bpm);
				minBpm = Math.min(...bpms);
				maxBpm = Math.max(...bpms);
				hourlyData = processHourlyData(heartRates);
			} else {
				minBpm = 0;
				maxBpm = 0;
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

</script>

<a
	href="/heartrate"
	class="tile block cursor-pointer group"
>
	<div class="flex items-center justify-between mb-3">
		<h3 class="text-sm font-medium text-theme-text-secondary uppercase tracking-wide">Heart Rate</h3>
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
			<!-- Heart Icon with Pulse -->
			<div class="relative flex items-center justify-center w-20 h-20">
				<svg class="w-12 h-12" fill={colors.heartrate} viewBox="0 0 24 24">
					<path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/>
				</svg>
				{#if restingBpm > 0}
					<div class="absolute inset-0 flex items-center justify-center animate-pulse">
						<svg class="w-14 h-14 opacity-30" fill={colors.heartrate} viewBox="0 0 24 24">
							<path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/>
						</svg>
					</div>
				{/if}
			</div>

			<!-- Stats -->
			<div class="flex-1">
				{#if restingBpm > 0}
					<p class="text-3xl font-bold text-theme-text">{restingBpm} <span class="text-lg text-theme-text-secondary">bpm</span></p>
					<p class="text-sm mt-1 text-theme-text-secondary">Resting</p>
				{:else if minBpm > 0}
					<p class="text-3xl font-bold text-theme-text">-- <span class="text-lg text-theme-text-secondary">bpm</span></p>
					<p class="text-sm mt-1 text-theme-text-secondary">Resting</p>
				{:else}
					<p class="text-xl font-bold text-theme-text-muted">No data</p>
				{/if}
			</div>
		</div>

		<!-- Stats Row -->
		{#if minBpm > 0}
			<div class="flex justify-around mt-4 text-sm">
				<div class="text-center">
					<p class="text-theme-text-muted">Min</p>
					<p class="text-theme-text font-medium">{minBpm}</p>
				</div>
				<div class="text-center">
					<p class="text-theme-text-muted">Max</p>
					<p class="text-theme-text font-medium">{maxBpm}</p>
				</div>
			</div>
		{/if}

		<!-- Mini Hourly Chart -->
		<div class="mt-4">
			<p class="text-xs text-theme-text-muted mb-2">Today's Heart Rate</p>
			<MiniBarChart data={hourlyData} color={colors.heartrate} height={32} />
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
