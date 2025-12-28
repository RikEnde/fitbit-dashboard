<script lang="ts">
	import {
		StepsTile,
		CaloriesTile,
		DistanceTile,
		HeartRateTile,
		SleepTile,
		ActiveMinutesTile
	} from '$components/tiles';
	import { preferences } from '$stores/preferences';
</script>

<svelte:head>
	<title>Dashboard | Fitbit</title>
</svelte:head>

<div class="p-4 sm:p-6 lg:p-8">
	<!-- Tile Grid -->
	<div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
		<!-- Steps Tile -->
		{#if $preferences.visibleTiles.includes('steps')}
			<StepsTile goal={10000} />
		{/if}

		<!-- Calories Tile -->
		{#if $preferences.visibleTiles.includes('calories')}
			<CaloriesTile goal={2000} />
		{/if}

		<!-- Distance Tile -->
		{#if $preferences.visibleTiles.includes('distance')}
			<DistanceTile goal={8} />
		{/if}

		<!-- Active Minutes Tile -->
		{#if $preferences.visibleTiles.includes('active-minutes')}
			<ActiveMinutesTile goal={30} />
		{/if}

		<!-- Heart Rate Tile -->
		{#if $preferences.visibleTiles.includes('heart-rate')}
			<HeartRateTile />
		{/if}

		<!-- Sleep Tile -->
		{#if $preferences.visibleTiles.includes('sleep')}
			<SleepTile goal={480} />
		{/if}
	</div>

	<!-- Quick Actions -->
	<div class="mt-8 flex flex-wrap gap-2">
		<a
			href="/exercise"
			class="px-4 py-2 bg-dark-card border border-dark-border rounded-lg text-sm text-gray-300 hover:border-gray-500 transition-colors"
		>
			View Exercises
		</a>
		<button
			onclick={() => preferences.reset()}
			class="px-4 py-2 bg-dark-card border border-dark-border rounded-lg text-sm text-gray-300 hover:border-gray-500 transition-colors"
		>
			Reset Layout
		</button>
	</div>
</div>
