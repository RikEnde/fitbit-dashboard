<script lang="ts">
    import '../app.css';
    import Header from '$components/layout/Header.svelte';
    import Login from '$components/Login.svelte';
    import {setContextClient} from '@urql/svelte';
    import {client} from '$graphql/client';
    import {credentials} from '$stores/auth';
    import {profile, profileError, profileLoading} from '$stores/profile';
    import {preferences} from '$stores/preferences';
    import {PROFILE_QUERY, LATEST_DATA_DATE_QUERY} from '$graphql/queries';
    import {setDate} from '$stores/dashboard';
    import type {Snippet} from 'svelte';
    import {onMount} from 'svelte';

    interface Props {
		children: Snippet;
	}

	let { children }: Props = $props();

	// Set up URQL client for the entire app
	setContextClient(client);

	// Sync theme class with preferences store
	onMount(() => {
		const unsub = preferences.subscribe((prefs) => {
			document.documentElement.classList.toggle('dark', prefs.theme === 'dark');
		});
		return unsub;
	});

	// Load profile when credentials change
	onMount(() => {
		const unsub = credentials.subscribe(async (creds) => {
			if (!creds) return;
			profileLoading.set(true);
			try {
				const result = await client.query(PROFILE_QUERY, {}).toPromise();
				if (result.error) {
					profileError.set(result.error.message);
				} else if (result.data?.profile) {
					profile.set(result.data.profile);

					// Set selected date to the most recent date with data
					const dateResult = await client.query(LATEST_DATA_DATE_QUERY, {}).toPromise();
					if (dateResult.data?.latestDataDate) {
						setDate(new Date(dateResult.data.latestDataDate + 'T12:00:00'));
					}
				}
			} catch (err) {
				profileError.set(err instanceof Error ? err.message : 'Failed to load profile');
			} finally {
				profileLoading.set(false);
			}
		});
		return unsub;
	});
</script>

{#if $credentials}
	<div class="min-h-screen bg-theme-bg text-theme-text">
		<Header />
		<main class="max-w-7xl mx-auto">
			{#if !$profileLoading && !$profile}
				<div class="flex items-center justify-center py-24">
					<div class="bg-theme-card rounded-2xl shadow-xl p-8 max-w-md text-center">
						<h2 class="text-xl font-bold text-theme-text mb-3">Welcome!</h2>
						<p class="text-theme-muted">To get started, import your Fitbit data using the "Import Data" option in the menu above.</p>
					</div>
				</div>
			{:else}
				{@render children()}
			{/if}
		</main>
	</div>
{:else}
	<Login />
{/if}
