<script lang="ts">
    import {format} from 'date-fns';
    import {sleepStageColors} from '$utils/colors';

    interface SleepLevelData {
		dateTime: string;
		level: string;
		seconds: number;
	}

	interface Props {
		data: SleepLevelData[];
		startTime: string;
		endTime: string;
		height?: number;
	}

	let {
		data,
		startTime,
		endTime,
		height = 120
	}: Props = $props();

	// Calculate total duration in seconds
	let totalDuration = $derived.by(() => {
		const start = new Date(startTime).getTime();
		const end = new Date(endTime).getTime();
		return (end - start) / 1000;
	});

	// Stage levels from top to bottom: Awake, REM, Light, Deep
	const stageLevels = ['wake', 'rem', 'light', 'deep'];
	const stageHeight = (height - 40) / 4; // 40px for labels

	function getStageY(level: string): number {
		const index = stageLevels.indexOf(level.toLowerCase());
		return index >= 0 ? 20 + index * stageHeight : 20;
	}

	function getStageColor(level: string): string {
		const l = level.toLowerCase();
		if (l === 'wake' || l === 'awake') return sleepStageColors.awake;
		if (l === 'rem') return sleepStageColors.rem;
		if (l === 'light') return sleepStageColors.light;
		if (l === 'deep') return sleepStageColors.deep;
		return '#666';
	}

	// Process segments for visualization
	let segments = $derived.by(() => {
		if (data.length === 0) return [];

		const startMs = new Date(startTime).getTime();
		const result = [];

		for (let i = 0; i < data.length; i++) {
			const segment = data[i];
			const segmentStart = new Date(segment.dateTime).getTime();
			const segmentEnd = segmentStart + segment.seconds * 1000;

			const x1Pct = ((segmentStart - startMs) / 1000 / totalDuration) * 100;
			const x2Pct = ((segmentEnd - startMs) / 1000 / totalDuration) * 100;

			result.push({
				level: segment.level,
				x1: Math.max(0, x1Pct),
				x2: Math.min(100, x2Pct),
				y: getStageY(segment.level),
				color: getStageColor(segment.level),
				duration: segment.seconds
			});
		}

		return result;
	});

	// Time labels
	let timeLabels = $derived.by(() => {
		const start = new Date(startTime);
		const end = new Date(endTime);
		return {
			start: format(start, 'h:mm a'),
			end: format(end, 'h:mm a')
		};
	});

	// Generate connecting lines between segments
	let connectionLines = $derived.by(() => {
		const lines = [];
		for (let i = 0; i < segments.length - 1; i++) {
			const current = segments[i];
			const next = segments[i + 1];

			if (current.y !== next.y) {
				lines.push({
					x: current.x2,
					y1: current.y + stageHeight / 2,
					y2: next.y + stageHeight / 2
				});
			}
		}
		return lines;
	});
</script>

<div class="w-full">
	{#if data.length === 0}
		<div class="w-full flex items-center justify-center text-gray-500 text-sm" style="height: {height}px;">
			No sleep stage data available
		</div>
	{:else}
		<div class="relative" style="height: {height}px;">
			<!-- Stage labels on the left -->
			<div class="absolute left-0 top-0 bottom-10 w-12 flex flex-col justify-between text-[10px] text-gray-500">
				<span style="color: {sleepStageColors.awake}">Awake</span>
				<span style="color: {sleepStageColors.rem}">REM</span>
				<span style="color: {sleepStageColors.light}">Light</span>
				<span style="color: {sleepStageColors.deep}">Deep</span>
			</div>

			<!-- Chart area -->
			<div class="ml-14 h-full">
				<svg width="100%" height={height - 20} class="overflow-visible">
					<!-- Background grid lines -->
					{#each stageLevels as _, i}
						<line
							x1="0"
							y1={20 + i * stageHeight + stageHeight / 2}
							x2="100%"
							y2={20 + i * stageHeight + stageHeight / 2}
							stroke="#3d3d5c"
							stroke-dasharray="2,4"
						/>
					{/each}

					<!-- Sleep stage segments -->
					{#each segments as segment}
						<rect
							x="{segment.x1}%"
							y={segment.y}
							width="{Math.max(0.5, segment.x2 - segment.x1)}%"
							height={stageHeight - 4}
							rx="2"
							fill={segment.color}
							opacity="0.8"
						>
							<title>{segment.level}: {Math.round(segment.duration / 60)}m</title>
						</rect>
					{/each}

					<!-- Connection lines between stages -->
					{#each connectionLines as line}
						<line
							x1="{line.x}%"
							y1={line.y1}
							x2="{line.x}%"
							y2={line.y2}
							stroke="#666"
							stroke-width="1"
						/>
					{/each}
				</svg>

				<!-- Time labels -->
				<div class="flex justify-between text-[10px] text-gray-500 mt-1">
					<span>{timeLabels.start}</span>
					<span>{timeLabels.end}</span>
				</div>
			</div>
		</div>
	{/if}
</div>
