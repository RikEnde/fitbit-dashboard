<script lang="ts">
    import {onMount} from 'svelte';
    import {gql} from '@urql/svelte';
    import {client} from '$graphql/client';
    import {dateRange} from '$stores/dashboard';
    import {colors} from '$utils/colors';
    import {formatNumber} from '$utils/formatters';
    import ProgressRing from '$components/charts/ProgressRing.svelte';
    import MiniBarChart from '$components/charts/MiniBarChart.svelte';

    interface Props {
		goal?: number;
	}

	let { goal = 2000 }: Props = $props();

	// State
	let totalCalories = $state(0);
	let loading = $state(true);
	let error = $state<string | null>(null);
	let hourlyData = $state<{ label: string; value: number }[]>([]);

	// Query for calories data
	const CALORIES_QUERY = gql`
		query Calories($limit: Int, $range: DateRange) {
			calories(limit: $limit, range: $range) {
				id
				value
				dateTime
			}
		}
	`;

	interface CaloriesRecord {
		id: string;
		value: number;
		dateTime: string;
	}

	function processHourlyData(calories: CaloriesRecord[]): { label: string; value: number }[] {
		const hourlyMap = new Map<number, number>();
		for (let i = 0; i < 24; i++) {
			hourlyMap.set(i, 0);
		}
		for (const cal of calories) {
			const hour = new Date(cal.dateTime).getHours();
			hourlyMap.set(hour, (hourlyMap.get(hour) ?? 0) + cal.value);
		}
		return Array.from(hourlyMap.entries())
			.sort((a, b) => a[0] - b[0])
			.map(([hour, value]) => ({
				label: `${hour}:00`,
				value: Math.round(value)
			}));
	}

	async function fetchData(range: { from: string; to: string }) {
		loading = true;
		error = null;

		try {
			const result = await client.query(CALORIES_QUERY, { limit: 1440, range }).toPromise();
			if (result.error) {
				error = result.error.message;
				return;
			}

			const caloriesData = result.data?.calories ?? [];
			totalCalories = Math.round(caloriesData.reduce((sum: number, c: CaloriesRecord) => sum + c.value, 0));
			hourlyData = processHourlyData(caloriesData);
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

	let percentage = $derived(goal > 0 ? Math.round((totalCalories / goal) * 100) : 0);
</script>

<a
	href="/calories"
	class="tile block cursor-pointer group"
>
	<div class="flex items-center justify-between mb-3">
		<h3 class="text-sm font-medium text-gray-400 uppercase tracking-wide">Calories</h3>
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
				value={totalCalories}
				max={goal}
				size={80}
				strokeWidth={6}
				color={colors.calories}
			>
				<svg class="w-6 h-6 text-fitbit-calories" fill="currentColor" viewBox="0 0 24 24">
					<path d="M13.5 1.5c-.8 0-1.5.7-1.5 1.5v5c0 .3-.2.5-.5.5s-.5-.2-.5-.5v-4c0-.8-.7-1.5-1.5-1.5s-1.5.7-1.5 1.5v4c0 .3-.2.5-.5.5s-.5-.2-.5-.5v-3c0-.8-.7-1.5-1.5-1.5s-1.5.7-1.5 1.5v6c0 3.9 3.1 7 7 7s7-3.1 7-7v-8c0-.8-.7-1.5-1.5-1.5s-1.5.7-1.5 1.5v5c0 .3-.2.5-.5.5s-.5-.2-.5-.5v-6c0-.8-.7-1.5-1.5-1.5z"/>
				</svg>
			</ProgressRing>

			<!-- Stats -->
			<div class="flex-1">
				<p class="text-3xl font-bold text-white">{formatNumber(totalCalories)}</p>
				<p class="text-sm text-gray-400 mt-1">
					Goal: {formatNumber(goal)}
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
			<p class="text-xs text-gray-500 mb-2">Today's Burn</p>
			<MiniBarChart data={hourlyData} color={colors.calories} height={32} />
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
