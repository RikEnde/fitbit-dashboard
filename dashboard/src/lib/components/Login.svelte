<script lang="ts">
	import {login} from '$stores/auth';

	let username = $state('');
	let password = $state('');
	let confirmPassword = $state('');
	let error = $state('');
	let loading = $state(false);
	let mode = $state<'login' | 'register'>('login');

	async function handleSubmit() {
		error = '';

		if (mode === 'register') {
			if (password !== confirmPassword) {
				error = 'Passwords do not match';
				return;
			}
		}

		loading = true;

		try {
			if (mode === 'register') {
				const response = await fetch('/api/register', {
					method: 'POST',
					headers: {'Content-Type': 'application/json'},
					body: JSON.stringify({username, password})
				});
				const data = await response.json();
				if (!response.ok) {
					error = data.error || `Registration failed (${response.status})`;
					return;
				}
			}

			// Login (or auto-login after registration)
			const encoded = btoa(`${username}:${password}`);
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

	function toggleMode() {
		mode = mode === 'login' ? 'register' : 'login';
		error = '';
		confirmPassword = '';
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
			<div class="mb-4">
				<label for="password" class="block text-sm font-medium text-theme-muted mb-1">Password</label>
				<input
					id="password"
					type="password"
					bind:value={password}
					class="w-full px-3 py-2 rounded-lg bg-theme-bg border border-theme-border text-theme-text focus:outline-none focus:ring-2 focus:ring-fitbit-blue"
					required
				/>
			</div>
			{#if mode === 'register'}
				<div class="mb-4">
					<label for="confirm-password" class="block text-sm font-medium text-theme-muted mb-1">Confirm Password</label>
					<input
						id="confirm-password"
						type="password"
						bind:value={confirmPassword}
						class="w-full px-3 py-2 rounded-lg bg-theme-bg border border-theme-border text-theme-text focus:outline-none focus:ring-2 focus:ring-fitbit-blue"
						required
					/>
				</div>
			{/if}
			{#if error}
				<p class="text-red-500 text-sm mb-4">{error}</p>
			{/if}
			<button
				type="submit"
				disabled={loading}
				class="w-full py-2 px-4 bg-fitbit-blue text-white rounded-lg font-medium hover:bg-blue-600 disabled:opacity-50 transition-colors"
			>
				{#if loading}
					{mode === 'login' ? 'Signing in...' : 'Creating account...'}
				{:else}
					{mode === 'login' ? 'Sign in' : 'Create Account'}
				{/if}
			</button>
		</form>
		<p class="text-sm text-theme-muted text-center mt-4">
			{#if mode === 'login'}
				Don't have an account?
				<button onclick={toggleMode} class="text-fitbit-blue hover:underline">Create one</button>
			{:else}
				Already have an account?
				<button onclick={toggleMode} class="text-fitbit-blue hover:underline">Sign in</button>
			{/if}
		</p>
	</div>
</div>
