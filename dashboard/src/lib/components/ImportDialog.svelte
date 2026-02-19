<script lang="ts">
	import {get} from 'svelte/store';
	import {authHeader} from '$stores/auth';
	import {importJob, uploadZipFile} from '$stores/import';
	import type {ImportJobState, ImportResponse, UploadProgress} from '$stores/import';

	interface Props {
		onClose: () => void;
	}

	let {onClose}: Props = $props();

	type ImportMode = 'upload' | 'filesystem';

	const statTypes: Record<string, string> = {
		profile: 'Profile',
		heartrate: 'Heart Rate',
		steps: 'Steps',
		calories: 'Calories',
		distance: 'Distance',
		restingheartrate: 'Resting Heart Rate',
		exercise: 'Exercise',
		timeinzone: 'Time in Zone',
		activityminutes: 'Activity Minutes',
		activezoneminutes: 'Active Zone Minutes',
		vo2max: 'VO2 Max',
		runvo2max: 'Run VO2 Max',
		activitygoals: 'Activity Goals',
		sleep: 'Sleep',
		devicetemperature: 'Device Temperature',
		respiratoryrate: 'Respiratory Rate',
		hrv: 'HRV',
		hrvdetails: 'HRV Details',
		minutespo2: 'Minute SpO2',
		sleepscore: 'Sleep Score',
		computedtemperature: 'Computed Temperature',
		respiratoryratesummary: 'Respiratory Rate Summary',
		dailyspo2: 'Daily SpO2'
	};

	let mode = $state<ImportMode>('upload');
	let dataDir = $state('../data');
	let userName = $state('');
	let selectedFile = $state<File | null>(null);
	let selectedStats = $state<Set<string>>(new Set(Object.keys(statTypes)));
	let loading = $state(false);
	let error = $state('');
	let progressMessage = $state('');
	let uploadProgress = $state<UploadProgress | null>(null);
	let completed = $state(false);
	let completedResults = $state<ImportResponse | null>(null);
	let pollInterval: ReturnType<typeof setInterval> | null = null;

	let allSelected = $derived(selectedStats.size === Object.keys(statTypes).length);

	function handleBackdropClick(event: MouseEvent) {
		if (event.target === event.currentTarget) {
			handleClose();
		}
	}

	function toggleAll() {
		if (allSelected) {
			selectedStats = new Set();
		} else {
			selectedStats = new Set(Object.keys(statTypes));
		}
	}

	function toggleStat(stat: string) {
		const next = new Set(selectedStats);
		if (next.has(stat)) {
			next.delete(stat);
		} else {
			next.add(stat);
		}
		selectedStats = next;
	}

	function handleClose() {
		if (pollInterval && !loading) {
			clearInterval(pollInterval);
			pollInterval = null;
		}
		onClose();
	}

	function handleFileSelect(event: Event) {
		const input = event.target as HTMLInputElement;
		selectedFile = input.files?.[0] ?? null;
	}

	function formatBytes(bytes: number): string {
		if (bytes < 1024) return bytes + ' B';
		if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
		return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
	}

	async function handleImport() {
		if (mode === 'filesystem' && !userName.trim()) {
			error = 'Please enter a user name.';
			return;
		}
		if (mode === 'upload' && !selectedFile) {
			error = 'Please select a zip file.';
			return;
		}
		if (selectedStats.size === 0) {
			error = 'Please select at least one stat type.';
			return;
		}

		error = '';
		loading = true;
		completed = false;
		completedResults = null;
		uploadProgress = null;

		try {
			const header = get(authHeader);
			const stats = allSelected ? ['all'] : Array.from(selectedStats);
			let jobId: string;

			if (mode === 'upload') {
				progressMessage = 'Uploading zip file...';
				jobId = await uploadZipFile(
					selectedFile!,
					stats,
					header,
					(progress) => { uploadProgress = progress; }
				);
				uploadProgress = null;
				progressMessage = 'Upload complete. Processing...';
			} else {
				progressMessage = 'Starting import...';
				const response = await fetch('/api/import', {
					method: 'POST',
					headers: {
						'Content-Type': 'application/json',
						...(header ? {'Authorization': header} : {})
					},
					body: JSON.stringify({
						dataDir: dataDir,
						users: [userName.trim()],
						stats
					})
				});

				if (!response.ok) {
					throw new Error(`Import failed: ${response.status} ${response.statusText}`);
				}

				({jobId} = await response.json());
			}

			importJob.set({
				jobId,
				status: 'RUNNING',
				message: progressMessage,
				results: null,
				error: null
			});

			pollInterval = setInterval(() => pollJobStatus(jobId), 2000);
		} catch (e) {
			error = e instanceof Error ? e.message : 'Import failed.';
			loading = false;
			progressMessage = '';
			uploadProgress = null;
		}
	}

	async function pollJobStatus(jobId: string) {
		try {
			const header = get(authHeader);
			const response = await fetch(`/api/import/${jobId}`, {
				headers: header ? {'Authorization': header} : {}
			});

			if (!response.ok) {
				throw new Error(`Failed to check import status: ${response.status}`);
			}

			const job: ImportJobState = await response.json();

			importJob.set(job);

			if (job.status === 'RUNNING') {
				progressMessage = job.message || 'Importing...';
			} else if (job.status === 'COMPLETED') {
				if (pollInterval) {
					clearInterval(pollInterval);
					pollInterval = null;
				}
				loading = false;
				completed = true;
				completedResults = job.results;
				progressMessage = '';
			} else if (job.status === 'FAILED') {
				if (pollInterval) {
					clearInterval(pollInterval);
					pollInterval = null;
				}
				loading = false;
				error = job.error || 'Import failed.';
				progressMessage = '';
			}
		} catch (e) {
			if (pollInterval) {
				clearInterval(pollInterval);
				pollInterval = null;
			}
			loading = false;
			error = e instanceof Error ? e.message : 'Failed to check import status.';
			progressMessage = '';
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
	onkeydown={(e) => e.key === 'Escape' && handleClose()}
>
	<div class="bg-theme-card border border-theme-border rounded-xl shadow-xl w-full max-w-lg mx-4 max-h-[90vh] flex flex-col">
		<!-- Header -->
		<div class="flex items-center justify-between p-4 border-b border-theme-border shrink-0">
			<h2 class="text-lg font-semibold text-theme-text-bright">Import Data</h2>
			<button
				onclick={handleClose}
				class="text-theme-text-secondary hover:text-theme-text-bright transition-colors"
				aria-label="Close"
			>
				<svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
					<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
				</svg>
			</button>
		</div>

		<!-- Body -->
		<div class="p-4 space-y-4 overflow-y-auto">
			{#if completed && completedResults}
				<!-- Results summary -->
				<div class="space-y-3">
					<p class="text-sm text-green-500 font-medium">Import completed successfully!</p>
					{#each completedResults.results as userResult}
						<div>
							<p class="text-sm font-medium text-theme-text mb-1">{userResult.user}</p>
							<div class="grid grid-cols-2 gap-1 text-sm">
								{#each Object.entries(userResult.stats) as [stat, result]}
									{#if result.fileCount > 0}
										<span class="text-theme-text-secondary">{statTypes[stat] || stat}</span>
										<span class="text-theme-text">{result.fileCount} files</span>
									{/if}
								{/each}
							</div>
						</div>
					{/each}
				</div>
			{:else}
				<p class="text-sm text-theme-text-secondary">
					Import Fitbit data from a zip export or local files into the database.
				</p>

				<!-- Mode Toggle -->
				<div class="flex rounded-lg border border-theme-border overflow-hidden">
					<button
						onclick={() => mode = 'upload'}
						disabled={loading}
						class="flex-1 px-3 py-2 text-sm font-medium transition-colors {mode === 'upload' ? 'bg-fitbit-steps text-white' : 'bg-theme-bg text-theme-text-secondary hover:text-theme-text'} disabled:opacity-50"
					>
						Upload Zip
					</button>
					<button
						onclick={() => mode = 'filesystem'}
						disabled={loading}
						class="flex-1 px-3 py-2 text-sm font-medium transition-colors {mode === 'filesystem' ? 'bg-fitbit-steps text-white' : 'bg-theme-bg text-theme-text-secondary hover:text-theme-text'} disabled:opacity-50"
					>
						Filesystem
					</button>
				</div>

				{#if mode === 'upload'}
					<!-- File Upload -->
					<div>
						<label for="import-file" class="block text-sm font-medium text-theme-text mb-1">Fitbit Export Zip</label>
						<input
							id="import-file"
							type="file"
							accept=".zip"
							onchange={handleFileSelect}
							disabled={loading}
							class="w-full px-3 py-2 bg-theme-bg border border-theme-border rounded-lg text-theme-text text-sm file:mr-3 file:py-1 file:px-3 file:rounded file:border-0 file:text-sm file:bg-fitbit-steps/20 file:text-fitbit-steps hover:file:bg-fitbit-steps/30 disabled:opacity-50"
						/>
						{#if selectedFile}
							<p class="text-xs text-theme-text-secondary mt-1">{selectedFile.name} ({formatBytes(selectedFile.size)})</p>
						{/if}
					</div>
				{:else}
					<!-- Data Directory -->
					<div>
						<label for="import-datadir" class="block text-sm font-medium text-theme-text mb-1">Data Directory</label>
						<input
							id="import-datadir"
							type="text"
							bind:value={dataDir}
							disabled={loading}
							class="w-full px-3 py-2 bg-theme-bg border border-theme-border rounded-lg text-theme-text focus:outline-none focus:ring-2 focus:ring-fitbit-steps disabled:opacity-50"
						/>
					</div>

					<!-- User Name -->
					<div>
						<label for="import-user" class="block text-sm font-medium text-theme-text mb-1">User Name</label>
						<input
							id="import-user"
							type="text"
							bind:value={userName}
							disabled={loading}
							placeholder="Directory name of the user"
							class="w-full px-3 py-2 bg-theme-bg border border-theme-border rounded-lg text-theme-text placeholder:text-theme-text-muted focus:outline-none focus:ring-2 focus:ring-fitbit-steps disabled:opacity-50"
						/>
					</div>
				{/if}

				<!-- Stat Types -->
				<div>
					<div class="flex items-center justify-between mb-2">
						<span class="block text-sm font-medium text-theme-text">Stat Types</span>
						<button
							onclick={toggleAll}
							disabled={loading}
							class="text-xs text-fitbit-steps hover:underline disabled:opacity-50"
						>
							{allSelected ? 'Deselect All' : 'Select All'}
						</button>
					</div>
					<div class="grid grid-cols-2 gap-1 max-h-48 overflow-y-auto bg-theme-bg border border-theme-border rounded-lg p-2">
						{#each Object.entries(statTypes) as [key, label]}
							<label class="flex items-center gap-2 px-2 py-1 rounded hover:bg-theme-border cursor-pointer text-sm {loading ? 'opacity-50' : ''}">
								<input
									type="checkbox"
									checked={selectedStats.has(key)}
									onchange={() => toggleStat(key)}
									disabled={loading}
									class="accent-fitbit-steps"
								/>
								<span class="text-theme-text-secondary">{label}</span>
							</label>
						{/each}
					</div>
				</div>

				<!-- Upload Progress Bar -->
				{#if uploadProgress}
					<div class="space-y-1">
						<div class="flex justify-between text-xs text-theme-text-secondary">
							<span>Uploading... {uploadProgress.percent}%</span>
							<span>{formatBytes(uploadProgress.loaded)} / {formatBytes(uploadProgress.total)}</span>
						</div>
						<div class="w-full bg-theme-border rounded-full h-2">
							<div
								class="bg-fitbit-steps h-2 rounded-full transition-all duration-300"
								style="width: {uploadProgress.percent}%"
							></div>
						</div>
					</div>
				{/if}

				{#if progressMessage && !uploadProgress}
					<div class="flex items-center gap-2 text-sm text-fitbit-steps">
						<svg class="w-4 h-4 animate-spin" fill="none" viewBox="0 0 24 24">
							<circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
							<path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
						</svg>
						<span>{progressMessage}</span>
					</div>
				{/if}
			{/if}

			{#if error}
				<p class="text-sm text-red-500">{error}</p>
			{/if}
		</div>

		<!-- Footer -->
		<div class="flex justify-end gap-2 p-4 border-t border-theme-border shrink-0">
			<button
				onclick={handleClose}
				class="px-4 py-2 text-sm rounded-lg border border-theme-border text-theme-text hover:bg-theme-border transition-colors"
			>
				{completed ? 'Close' : 'Cancel'}
			</button>
			{#if !completed}
				<button
					onclick={handleImport}
					disabled={loading}
					class="px-4 py-2 text-sm rounded-lg bg-fitbit-steps text-white hover:bg-fitbit-steps/90 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
				>
					{#if loading}
						Importing...
					{:else}
						Import
					{/if}
				</button>
			{/if}
		</div>
	</div>
</div>
