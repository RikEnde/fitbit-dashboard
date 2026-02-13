<script lang="ts">
	import {login} from '$stores/auth';

	let username = $state('');
	let password = $state('');
	let error = $state('');
	let loading = $state(false);

	async function handleSubmit() {
		error = '';
		loading = true;

		// Test credentials with a raw fetch to /graphql
		const encoded = btoa(`${username}:${password}`);
		try {
			const response = await fetch('/graphql', {
				method: 'POST',
				headers: {
					'Content-Type': 'application/json',
					'Authorization': `Basic ${encoded}`
				},
				body: JSON.stringify({query: '{ __typename }'})
			});
			if (response.ok) {
				login(username, password);
			} else if (response.status === 401) {
				error = 'Invalid username or password';
			} else {
				error = `Server error (${response.status})`;
			}
		} catch {
			error = 'Could not connect to server';
		} finally {
			loading = false;
		}
	}
</script>

<div class="min-h-screen bg-theme-bg flex items-center justify-center">
	<div class="bg-theme-card rounded-2xl shadow-xl p-8 w-full max-w-sm">
		<h1 class="text-2xl font-bold text-theme-text mb-6 text-center">Fitbit Dashboard</h1>
		<form onsubmit={handleSubmit}>
			<div class="mb-4">
				<label for="username" class="block text-sm font-medium text-theme-muted mb-1">Username</label>
				<input
					id="username"
					type="text"
					bind:value={username}
					class="w-full px-3 py-2 rounded-lg bg-theme-bg border border-theme-border text-theme-text focus:outline-none focus:ring-2 focus:ring-fitbit-blue"
					required
				/>
			</div>
			<div class="mb-6">
				<label for="password" class="block text-sm font-medium text-theme-muted mb-1">Password</label>
				<input
					id="password"
					type="password"
					bind:value={password}
					class="w-full px-3 py-2 rounded-lg bg-theme-bg border border-theme-border text-theme-text focus:outline-none focus:ring-2 focus:ring-fitbit-blue"
					required
				/>
			</div>
			{#if error}
				<p class="text-red-500 text-sm mb-4">{error}</p>
			{/if}
			<button
				type="submit"
				disabled={loading}
				class="w-full py-2 px-4 bg-fitbit-blue text-white rounded-lg font-medium hover:bg-blue-600 disabled:opacity-50 transition-colors"
			>
				{loading ? 'Signing in...' : 'Sign in'}
			</button>
		</form>
	</div>
</div>
