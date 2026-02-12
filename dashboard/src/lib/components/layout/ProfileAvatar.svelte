<script lang="ts">
    import {profile, profileLoading} from '$stores/profile';
    import ProfileDropdown from './ProfileDropdown.svelte';

    let showDropdown = false;

	function toggleDropdown() {
		showDropdown = !showDropdown;
	}

	function closeDropdown() {
		showDropdown = false;
	}

	// Close dropdown when clicking outside
	function handleClickOutside(event: MouseEvent) {
		const target = event.target as HTMLElement;
		if (!target.closest('.profile-container')) {
			closeDropdown();
		}
	}
</script>

<svelte:window onclick={handleClickOutside} />

<div class="relative profile-container">
	<button
		onclick={toggleDropdown}
		class="w-8 h-8 rounded-full overflow-hidden border-2 border-theme-border hover:border-fitbit-steps transition-colors focus:outline-none focus:ring-2 focus:ring-fitbit-steps focus:ring-offset-2 focus:ring-offset-theme-bg"
		aria-label="Open profile menu"
	>
		{#if $profileLoading}
			<div class="w-full h-full bg-theme-border animate-pulse"></div>
		{:else if $profile?.avatar}
			<img
				src="data:image/jpeg;base64,{$profile.avatar}"
				alt={$profile.displayName || 'Profile'}
				class="w-full h-full object-cover"
			/>
		{:else}
			<div class="w-full h-full bg-fitbit-steps flex items-center justify-center text-white font-medium text-sm">
				{$profile?.firstName?.charAt(0) || '?'}
			</div>
		{/if}
	</button>

	{#if showDropdown}
		<ProfileDropdown onClose={closeDropdown} />
	{/if}
</div>
