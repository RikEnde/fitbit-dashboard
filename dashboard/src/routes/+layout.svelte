<script lang="ts">
    import '../app.css';
    import Header from '$components/layout/Header.svelte';
    import Login from '$components/Login.svelte';
    import {setContextClient} from '@urql/svelte';
    import {client} from '$graphql/client';
    import {credentials} from '$stores/auth';
    import {profile, profileError, profileLoading} from '$stores/profile';
    import {preferences} from '$stores/preferences';
    import {PROFILE_QUERY} from '$graphql/queries';
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
				if (result.data?.profiles?.[0]) {
					profile.set(result.data.profiles[0]);
				} else if (result.error) {
					profileError.set(result.error.message);
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
			{@render children()}
		</main>
	</div>
{:else}
	<Login />
{/if}
