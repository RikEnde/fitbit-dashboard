<script lang="ts">
	interface DataPoint {
		label: string;
		value: number;
	}

	interface Props {
		data: DataPoint[];
		color?: string;
		height?: number;
		showLabels?: boolean;
	}

	let {
		data,
		color = '#00B0B9',
		height = 40,
		showLabels = false
	}: Props = $props();

	let maxValue = $derived(Math.max(...data.map((d) => d.value), 1));
	let hasData = $derived(data.some((d) => d.value > 0));
</script>

<div class="w-full" style="height: {height}px;">
	{#if data.length === 0}
		<div class="w-full h-full flex items-center justify-center text-theme-text-muted text-xs">
			No data
		</div>
	{:else if !hasData}
		<div class="w-full h-full flex items-center justify-center text-theme-text-muted text-xs">
			No activity
		</div>
	{:else}
		<div class="w-full h-full flex items-end gap-px">
			{#each data as point, i}
				{@const barHeightPx = maxValue > 0 ? (point.value / maxValue) * height : 0}
				<div
					class="flex-1 flex flex-col items-center justify-end h-full"
					title="{point.label}: {point.value.toLocaleString()}"
				>
					<div
						class="w-full rounded-t-sm transition-all duration-300 hover:opacity-80"
						style="height: {barHeightPx}px; background-color: {color}; min-height: {point.value > 0 ? '2px' : '0'};"
					></div>
					{#if showLabels && i % 4 === 0}
						<span class="text-[8px] text-theme-text-muted mt-1">{point.label}</span>
					{/if}
				</div>
			{/each}
		</div>
	{/if}
</div>
