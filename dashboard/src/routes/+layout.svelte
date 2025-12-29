<script lang="ts">
    import '../app.css';
    import Header from '$components/layout/Header.svelte';
    import {setContextClient} from '@urql/svelte';
    import {client} from '$graphql/client';
    import {profile, profileError, profileLoading} from '$stores/profile';
    import {PROFILE_QUERY} from '$graphql/queries';
    import type {Snippet} from 'svelte';
    import {onMount} from 'svelte';

    interface Props {
		children: Snippet;
	}

	let { children }: Props = $props();

	// Set up URQL client for the entire app
	setContextClient(client);

	// Load profile on mount
	onMount(async () => {
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
</script>

<div class="min-h-screen bg-dark-bg text-white">
	<Header />
	<main class="max-w-7xl mx-auto">
		{@render children()}
	</main>
</div>
