<script lang="ts">
    import {format} from 'date-fns';

    interface DataPoint {
		date: string;
		value: number;
	}

	interface Props {
		data: DataPoint[];
		color?: string;
		height?: number;
		goal?: number;
		formatValue?: (value: number) => string;
		onBarClick?: (date: string) => void;
		selectedDate?: string | null;
	}

	let {
		data,
		color = '#00B0B9',
		height = 200,
		goal,
		formatValue = (v) => v.toLocaleString(),
		onBarClick,
		selectedDate = null
	}: Props = $props();

	let maxValue = $derived(Math.max(...data.map((d) => d.value), goal ?? 1, 1));
	let hasData = $derived(data.some((d) => d.value > 0));

	function formatDateLabel(dateStr: string): string {
		try {
			const date = new Date(dateStr);
			return format(date, 'MMM d');
		} catch {
			return dateStr;
		}
	}

	function handleBarClick(date: string) {
		if (onBarClick) {
			onBarClick(date);
		}
	}
</script>

<div class="w-full">
	{#if data.length === 0}
		<div class="w-full flex items-center justify-center text-gray-500 text-sm" style="height: {height}px;">
			No data available
		</div>
	{:else if !hasData}
		<div class="w-full flex items-center justify-center text-gray-500 text-sm" style="height: {height}px;">
			No activity recorded
		</div>
	{:else}
		<div class="relative w-full" style="height: {height}px;">
			<!-- Goal line -->
			{#if goal && goal > 0}
				{@const goalY = height - (goal / maxValue) * (height - 24)}
				<div
					class="absolute left-0 right-0 border-t-2 border-dashed border-gray-500 opacity-50 pointer-events-none"
					style="top: {goalY}px;"
				>
					<span class="absolute -top-5 right-0 text-xs text-gray-400">
						Goal: {formatValue(goal)}
					</span>
				</div>
			{/if}

			<!-- Bars -->
			<div class="w-full h-full flex items-end gap-1 pb-6">
				{#each data as point}
					{@const barHeight = maxValue > 0 ? (point.value / maxValue) * (height - 24) : 0}
					{@const isSelected = selectedDate === point.date}
					{@const meetsGoal = goal ? point.value >= goal : false}
					<button
						type="button"
						class="flex-1 flex flex-col items-center justify-end h-full group cursor-pointer"
						onclick={() => handleBarClick(point.date)}
					>
						<!-- Tooltip on hover -->
						<div class="absolute -top-8 left-1/2 transform -translate-x-1/2 bg-dark-card border border-dark-border rounded px-2 py-1 text-xs text-white opacity-0 group-hover:opacity-100 transition-opacity pointer-events-none whitespace-nowrap z-10">
							{formatDateLabel(point.date)}: {formatValue(point.value)}
						</div>

						<!-- Bar -->
						<div
							class="w-full rounded-t transition-all duration-200 {isSelected ? 'ring-2 ring-white ring-offset-2 ring-offset-dark-bg' : ''}"
							style="height: {Math.max(barHeight, point.value > 0 ? 4 : 0)}px; background-color: {meetsGoal ? '#22C55E' : color}; opacity: {isSelected ? 1 : 0.8};"
						></div>
					</button>
				{/each}
			</div>

			<!-- X-axis labels -->
			<div class="absolute bottom-0 left-0 right-0 flex gap-1">
				{#each data as point, i}
					{#if i === 0 || i === Math.floor(data.length / 2) || i === data.length - 1}
						<div class="flex-1 text-center">
							<span class="text-[10px] text-gray-500">{formatDateLabel(point.date)}</span>
						</div>
					{:else}
						<div class="flex-1"></div>
					{/if}
				{/each}
			</div>
		</div>
	{/if}
</div>
