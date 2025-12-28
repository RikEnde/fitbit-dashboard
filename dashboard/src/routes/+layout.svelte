<script lang="ts">
	import '../app.css';
	import Header from '$components/layout/Header.svelte';
	import { setContextClient } from '@urql/svelte';
	import { client } from '$graphql/client';
	import { profile, profileLoading, profileError } from '$stores/profile';
	import { PROFILE_QUERY } from '$graphql/queries';
	import { onMount } from 'svelte';
	import type { Snippet } from 'svelte';

	interface Props {
		children: Snippet;
	}

	let { children }: Props = $props();

	// Set up URQL client for the entire app
	setContextClient(client);

	// Load profile on mount
	onMount(async () => {
		// TODO: Get profile ID from configuration or first profile
		const profileId = '1'; // Default profile ID

		profileLoading.set(true);
		try {
			const result = await client.query(PROFILE_QUERY, { id: profileId }).toPromise();
			if (result.data?.profile) {
				profile.set(result.data.profile);
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
