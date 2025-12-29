<script lang="ts">
    import type {Snippet} from 'svelte';

    interface Props {
		title: string;
		href?: string;
		color?: string;
		loading?: boolean;
		error?: string | null;
		size?: 'small' | 'medium' | 'large';
		children: Snippet;
	}

	let {
		title,
		href,
		color = '#00B0B9',
		loading = false,
		error = null,
		size = 'small',
		children
	}: Props = $props();

	const sizeClasses = {
		small: 'col-span-1',
		medium: 'col-span-1 sm:col-span-2',
		large: 'col-span-1 sm:col-span-2 lg:col-span-4'
	};
</script>

{#if href}
	<a
		{href}
		class="tile block {sizeClasses[size]} cursor-pointer group"
		style="--tile-color: {color}"
	>
		<div class="flex items-center justify-between mb-3">
			<h3 class="text-sm font-medium text-gray-400 uppercase tracking-wide">{title}</h3>
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
			</div>
		{:else if error}
			<div class="text-red-400 text-sm">{error}</div>
		{:else}
			{@render children()}
		{/if}
	</a>
{:else}
	<div class="tile {sizeClasses[size]}" style="--tile-color: {color}">
		<div class="flex items-center justify-between mb-3">
			<h3 class="text-sm font-medium text-gray-400 uppercase tracking-wide">{title}</h3>
		</div>

		{#if loading}
			<div class="animate-pulse space-y-3">
				<div class="h-8 bg-dark-border rounded w-1/2"></div>
				<div class="h-4 bg-dark-border rounded w-3/4"></div>
			</div>
		{:else if error}
			<div class="text-red-400 text-sm">{error}</div>
		{:else}
			{@render children()}
		{/if}
	</div>
{/if}
