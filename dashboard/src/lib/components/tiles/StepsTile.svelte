<script lang="ts">
    import {onMount} from 'svelte';
    import {client} from '$graphql/client';
    import {DAILY_STEPS_SUM_QUERY, STEPS_QUERY as HOURLY_STEPS_QUERY} from '$graphql/queries';
    import {dateRange} from '$stores/dashboard';
    import {colors} from '$utils/colors';
    import {formatNumber} from '$utils/formatters';
    import ProgressRing from '$components/charts/ProgressRing.svelte';
    import MiniBarChart from '$components/charts/MiniBarChart.svelte';

    interface Props {
		goal?: number;
	}

	let { goal = 10000 }: Props = $props();

	// State
	let totalSteps = $state(0);
	let loading = $state(true);
	let error = $state<string | null>(null);
	let hourlyData = $state<{ label: string; value: number }[]>([]);

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
				label: `${hour}:00`,
				value
			}));
	}

	async function fetchData(range: { from: string; to: string }) {
		loading = true;
		error = null;

		try {
			// Fetch daily total
			const dailyResult = await client.query(DAILY_STEPS_SUM_QUERY, { range }).toPromise();
			if (dailyResult.error) {
				error = dailyResult.error.message;
				return;
			}
			totalSteps = dailyResult.data?.dailyStepsSum?.[0]?.totalSteps ?? 0;

			// Fetch hourly breakdown
			const hourlyResult = await client.query(HOURLY_STEPS_QUERY, { limit: 1440, range }).toPromise();
			if (hourlyResult.data?.steps) {
				hourlyData = processHourlyData(hourlyResult.data.steps);
			}
		} catch (e) {
			error = e instanceof Error ? e.message : 'Failed to fetch data';
		} finally {
			loading = false;
		}
	}

	// Fetch data on mount and when date changes
	onMount(() => {
		const unsubscribe = dateRange.subscribe((range) => {
			fetchData(range);
		});
		return unsubscribe;
	});

	let percentage = $derived(goal > 0 ? Math.round((totalSteps / goal) * 100) : 0);
</script>

<a
	href="/steps"
	class="tile block cursor-pointer group"
>
	<div class="flex items-center justify-between mb-3">
		<h3 class="text-sm font-medium text-theme-text-secondary uppercase tracking-wide">Steps</h3>
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
				value={totalSteps}
				max={goal}
				size={80}
				strokeWidth={6}
				color={colors.steps}
			>
				<svg class="w-6 h-6 text-fitbit-steps" fill="currentColor" viewBox="0 0 24 24">
					<path d="M13.5 5.5c1.09 0 2-.81 2-1.81s-.91-1.69-2-1.69-2 .59-2 1.59.91 1.91 2 1.91zM17.5 10.78c-1.23-.89-2.62-1.35-4.05-1.35-.71 0-1.4.11-2.05.32L10 7.5c-.2-.55-.7-.94-1.28-.94-.83 0-1.5.67-1.5 1.5 0 .19.04.38.11.55l2.44 5.94c.22.53.64.94 1.14 1.14L11 16.5v4c0 .83.67 1.5 1.5 1.5s1.5-.67 1.5-1.5v-4.88l1.87-4.68c.25-.63-.02-1.34-.56-1.66z"/>
				</svg>
			</ProgressRing>

			<!-- Stats -->
			<div class="flex-1">
				<p class="text-3xl font-bold text-theme-text">{formatNumber(totalSteps)}</p>
				<p class="text-sm text-theme-text-secondary mt-1">
					Goal: {formatNumber(goal)}
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
			<p class="text-xs text-theme-text-muted mb-2">Today's Activity</p>
			<MiniBarChart data={hourlyData} color={colors.steps} height={32} />
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
