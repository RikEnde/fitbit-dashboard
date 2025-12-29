<script lang="ts">
    import type {Snippet} from 'svelte';

    interface Props {
		value: number;
		max: number;
		size?: number;
		strokeWidth?: number;
		color?: string;
		backgroundColor?: string;
		showPercentage?: boolean;
		children?: Snippet;
	}

	let {
		value,
		max,
		size = 120,
		strokeWidth = 8,
		color = '#00B0B9',
		backgroundColor = '#3d3d5c',
		showPercentage = false,
		children
	}: Props = $props();

	let radius = $derived((size - strokeWidth) / 2);
	let circumference = $derived(radius * 2 * Math.PI);

	// Calculate percentage (capped at 100% for display, but can exceed)
	let percentage = $derived(max > 0 ? Math.min((value / max) * 100, 100) : 0);
	let actualPercentage = $derived(max > 0 ? (value / max) * 100 : 0);
	let offset = $derived(circumference - (percentage / 100) * circumference);
	let exceededGoal = $derived(value >= max);
</script>

<div class="relative inline-flex items-center justify-center" style="width: {size}px; height: {size}px;">
	<svg
		class="transform -rotate-90"
		width={size}
		height={size}
	>
		<!-- Background circle -->
		<circle
			cx={size / 2}
			cy={size / 2}
			r={radius}
			stroke={backgroundColor}
			stroke-width={strokeWidth}
			fill="none"
		/>
		<!-- Progress circle -->
		<circle
			cx={size / 2}
			cy={size / 2}
			r={radius}
			stroke={color}
			stroke-width={strokeWidth}
			fill="none"
			stroke-linecap="round"
			stroke-dasharray={circumference}
			stroke-dashoffset={offset}
			class="transition-all duration-500 ease-out"
		/>
	</svg>

	<!-- Center content -->
	<div class="absolute inset-0 flex flex-col items-center justify-center">
		{#if showPercentage}
			<span class="text-lg font-bold" style="color: {color}">
				{Math.round(actualPercentage)}%
			</span>
			{#if exceededGoal}
				<span class="text-xs text-green-400">Goal reached!</span>
			{/if}
		{:else if children}
			{@render children()}
		{/if}
	</div>
</div>
