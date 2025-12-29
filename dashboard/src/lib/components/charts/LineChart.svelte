<script lang="ts">
	import { format } from 'date-fns';

	interface DataPoint {
		time: string;
		value: number;
	}

	interface Props {
		data: DataPoint[];
		color?: string;
		height?: number;
		showArea?: boolean;
		formatValue?: (value: number) => string;
		formatTime?: (time: string) => string;
		minY?: number;
		maxY?: number;
	}

	let {
		data,
		color = '#EF4444',
		height = 200,
		showArea = true,
		formatValue = (v) => v.toString(),
		formatTime = (t) => {
			try {
				return format(new Date(t), 'h:mm a');
			} catch {
				return t;
			}
		},
		minY,
		maxY
	}: Props = $props();

	let containerWidth = $state(800);
	let containerRef: HTMLDivElement;

	// Padding for axes
	const paddingLeft = 45;
	const paddingRight = 10;
	const paddingTop = 20;
	const paddingBottom = 30;

	let chartWidth = $derived(containerWidth - paddingLeft - paddingRight);
	let chartHeight = $derived(height - paddingTop - paddingBottom);

	let values = $derived(data.map((d) => d.value));
	let computedMinY = $derived(minY ?? Math.min(...values, 40));
	let computedMaxY = $derived(maxY ?? Math.max(...values, 100));
	let yRange = $derived(computedMaxY - computedMinY);

	let hasData = $derived(data.length > 0 && data.some((d) => d.value > 0));

	// Generate path for the line
	let linePath = $derived.by(() => {
		if (data.length === 0) return '';

		const points = data.map((d, i) => {
			const x = paddingLeft + (i / (data.length - 1)) * chartWidth;
			const y = paddingTop + chartHeight - ((d.value - computedMinY) / yRange) * chartHeight;
			return `${x},${y}`;
		});

		return `M ${points.join(' L ')}`;
	});

	// Generate path for the filled area
	let areaPath = $derived.by(() => {
		if (data.length === 0) return '';

		const points = data.map((d, i) => {
			const x = paddingLeft + (i / (data.length - 1)) * chartWidth;
			const y = paddingTop + chartHeight - ((d.value - computedMinY) / yRange) * chartHeight;
			return `${x},${y}`;
		});

		const baseY = paddingTop + chartHeight;
		const firstX = paddingLeft;
		const lastX = paddingLeft + chartWidth;

		return `M ${firstX},${baseY} L ${points.join(' L ')} L ${lastX},${baseY} Z`;
	});

	// Y-axis labels
	let yLabels = $derived.by(() => {
		const labels = [];
		const step = yRange / 4;
		for (let i = 0; i <= 4; i++) {
			const value = computedMinY + step * i;
			const y = paddingTop + chartHeight - (i / 4) * chartHeight;
			labels.push({ value: Math.round(value), y });
		}
		return labels;
	});

	// X-axis labels (first, middle, last)
	let xLabels = $derived.by(() => {
		if (data.length === 0) return [];
		const labels = [];
		const indices = [0, Math.floor(data.length / 2), data.length - 1];

		for (const i of indices) {
			if (data[i]) {
				const x = paddingLeft + (i / (data.length - 1)) * chartWidth;
				labels.push({ time: formatTime(data[i].time), x });
			}
		}
		return labels;
	});

	// Hover state
	let hoverIndex = $state<number | null>(null);
	let hoverPoint = $derived.by(() => {
		if (hoverIndex === null || !data[hoverIndex]) return null;
		const d = data[hoverIndex];
		const x = paddingLeft + (hoverIndex / (data.length - 1)) * chartWidth;
		const y = paddingTop + chartHeight - ((d.value - computedMinY) / yRange) * chartHeight;
		return { x, y, value: d.value, time: d.time };
	});

	function handleMouseMove(event: MouseEvent) {
		if (!containerRef || data.length === 0) return;
		const rect = containerRef.getBoundingClientRect();
		const mouseX = event.clientX - rect.left - paddingLeft;
		const index = Math.round((mouseX / chartWidth) * (data.length - 1));
		hoverIndex = Math.max(0, Math.min(data.length - 1, index));
	}

	function handleMouseLeave() {
		hoverIndex = null;
	}

	$effect(() => {
		if (containerRef) {
			const observer = new ResizeObserver((entries) => {
				containerWidth = entries[0].contentRect.width;
			});
			observer.observe(containerRef);
			return () => observer.disconnect();
		}
	});
</script>

<div
	class="w-full"
	bind:this={containerRef}
	onmousemove={handleMouseMove}
	onmouseleave={handleMouseLeave}
	role="img"
	aria-label="Heart rate chart"
>
	{#if !hasData}
		<div class="w-full flex items-center justify-center text-gray-500 text-sm" style="height: {height}px;">
			No data available
		</div>
	{:else}
		<svg width="100%" {height} class="overflow-visible">
			<!-- Grid lines -->
			{#each yLabels as label}
				<line
					x1={paddingLeft}
					y1={label.y}
					x2={paddingLeft + chartWidth}
					y2={label.y}
					stroke="#3d3d5c"
					stroke-dasharray="4,4"
				/>
				<text
					x={paddingLeft - 8}
					y={label.y + 4}
					text-anchor="end"
					class="text-[10px] fill-gray-500"
				>
					{label.value}
				</text>
			{/each}

			<!-- Area fill -->
			{#if showArea}
				<path
					d={areaPath}
					fill={color}
					fill-opacity="0.2"
				/>
			{/if}

			<!-- Line -->
			<path
				d={linePath}
				fill="none"
				stroke={color}
				stroke-width="2"
				stroke-linejoin="round"
				stroke-linecap="round"
			/>

			<!-- Hover point -->
			{#if hoverPoint}
				<circle
					cx={hoverPoint.x}
					cy={hoverPoint.y}
					r="6"
					fill={color}
					stroke="white"
					stroke-width="2"
				/>
				<!-- Tooltip -->
				<g transform="translate({hoverPoint.x}, {hoverPoint.y - 35})">
					<rect
						x="-40"
						y="-12"
						width="80"
						height="24"
						rx="4"
						fill="#252542"
						stroke="#3d3d5c"
					/>
					<text
						text-anchor="middle"
						y="4"
						class="text-xs fill-white font-medium"
					>
						{formatValue(hoverPoint.value)} · {formatTime(hoverPoint.time)}
					</text>
				</g>
			{/if}

			<!-- X-axis labels -->
			{#each xLabels as label}
				<text
					x={label.x}
					y={height - 8}
					text-anchor="middle"
					class="text-[10px] fill-gray-500"
				>
					{label.time}
				</text>
			{/each}
		</svg>
	{/if}
</div>
