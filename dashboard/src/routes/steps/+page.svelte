<script lang="ts">
	import { formattedDate } from '$stores/dashboard';
</script>

<svelte:head>
	<title>Steps | Fitbit Dashboard</title>
</svelte:head>

<div class="p-4 sm:p-6 lg:p-8">
	<div class="mb-6">
		<a href="/" class="text-fitbit-steps hover:underline text-sm">&larr; Back to Dashboard</a>
	</div>

	<h1 class="text-2xl font-bold text-white mb-2">Steps</h1>
	<p class="text-gray-400 mb-8">{$formattedDate}</p>

	<div class="bg-dark-card rounded-xl border border-dark-border p-6">
		<p class="text-gray-400 text-center py-12">
			Steps detail view coming soon...
		</p>
	</div>
</div>
