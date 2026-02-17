<script lang="ts">
	import {get} from 'svelte/store';
	import {authHeader} from '$stores/auth';

	interface Props {
		onClose: () => void;
	}

	let {onClose}: Props = $props();

	const exportTypes: Record<string, string> = {
		heartrate: 'Heart Rate',
		steps: 'Steps',
		calories: 'Calories',
		distance: 'Distance',
		sleep: 'Sleep'
	};

	let selectedType = $state('heartrate');
	let fromDate = $state('');
	let toDate = $state('');
	let loading = $state(false);
	let error = $state('');

	function handleBackdropClick(event: MouseEvent) {
		if (event.target === event.currentTarget) {
			onClose();
		}
	}

	async function handleExport() {
		if (!fromDate || !toDate) {
			error = 'Please select both from and to dates.';
			return;
		}

		error = '';
		loading = true;

		try {
			const from = `${fromDate}T00:00:00`;
			const to = `${toDate}T23:59:59`;
			const header = get(authHeader);

			const response = await fetch(`/api/export/${selectedType}?from=${from}&to=${to}`, {
				headers: header ? {'Authorization': header} : {}
			});

			if (!response.ok) {
				throw new Error(`Export failed: ${response.status} ${response.statusText}`);
			}

			const blob = await response.blob();
			const url = URL.createObjectURL(blob);
			const a = document.createElement('a');
			a.href = url;
			a.download = `apple-health-${selectedType}-${fromDate}-to-${toDate}.xml`;
			document.body.appendChild(a);
			a.click();
			document.body.removeChild(a);
			URL.revokeObjectURL(url);

			onClose();
		} catch (e) {
			error = e instanceof Error ? e.message : 'Export failed.';
		} finally {
			loading = false;
		}
	}
</script>

<!-- svelte-ignore a11y_no_noninteractive_element_interactions -->
<!-- svelte-ignore a11y_interactive_supports_focus -->
<div
	class="fixed inset-0 bg-black/50 flex items-center justify-center z-50"
	role="dialog"
	aria-modal="true"
	onclick={handleBackdropClick}
	onkeydown={(e) => e.key === 'Escape' && onClose()}
>
	<div class="bg-theme-card border border-theme-border rounded-xl shadow-xl w-full max-w-md mx-4">
		<!-- Header -->
		<div class="flex items-center justify-between p-4 border-b border-theme-border">
			<h2 class="text-lg font-semibold text-theme-text-bright">Export to Apple Health</h2>
			<button
				onclick={onClose}
				class="text-theme-text-secondary hover:text-theme-text-bright transition-colors"
				aria-label="Close"
			>
				<svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
					<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
				</svg>
			</button>
		</div>

		<!-- Body -->
		<div class="p-4 space-y-4">
			<p class="text-sm text-theme-text-secondary">
				Export your Fitbit data as Apple Health XML, compatible with the iOS Health Data Importer app.
			</p>

			<!-- Data Type -->
			<div>
				<label for="export-type" class="block text-sm font-medium text-theme-text mb-1">Data Type</label>
				<select
					id="export-type"
					bind:value={selectedType}
					class="w-full px-3 py-2 bg-theme-bg border border-theme-border rounded-lg text-theme-text focus:outline-none focus:ring-2 focus:ring-fitbit-steps"
				>
					{#each Object.entries(exportTypes) as [value, label]}
						<option {value}>{label}</option>
					{/each}
				</select>
			</div>

			<!-- Date Range -->
			<div class="grid grid-cols-2 gap-3">
				<div>
					<label for="export-from" class="block text-sm font-medium text-theme-text mb-1">From</label>
					<input
						id="export-from"
						type="date"
						bind:value={fromDate}
						class="w-full px-3 py-2 bg-theme-bg border border-theme-border rounded-lg text-theme-text focus:outline-none focus:ring-2 focus:ring-fitbit-steps"
					/>
				</div>
				<div>
					<label for="export-to" class="block text-sm font-medium text-theme-text mb-1">To</label>
					<input
						id="export-to"
						type="date"
						bind:value={toDate}
						class="w-full px-3 py-2 bg-theme-bg border border-theme-border rounded-lg text-theme-text focus:outline-none focus:ring-2 focus:ring-fitbit-steps"
					/>
				</div>
			</div>

			{#if error}
				<p class="text-sm text-red-500">{error}</p>
			{/if}
		</div>

		<!-- Footer -->
		<div class="flex justify-end gap-2 p-4 border-t border-theme-border">
			<button
				onclick={onClose}
				class="px-4 py-2 text-sm rounded-lg border border-theme-border text-theme-text hover:bg-theme-border transition-colors"
			>
				Cancel
			</button>
			<button
				onclick={handleExport}
				disabled={loading}
				class="px-4 py-2 text-sm rounded-lg bg-fitbit-steps text-white hover:bg-fitbit-steps/90 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
			>
				{#if loading}
					Exporting...
				{:else}
					Export
				{/if}
			</button>
		</div>
	</div>
</div>
