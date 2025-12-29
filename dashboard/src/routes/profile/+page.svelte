<script lang="ts">
    import {profile} from '$stores/profile';
    import {preferences} from '$stores/preferences';
    import {calculateAge, formatDate, formatHeight, formatStride, formatWeight} from '$utils/formatters';

    function handleThemeToggle() {
		preferences.toggleTheme();
	}

	// Format timezone for display
	function formatTimezone(tz: string): string {
		return tz.replace(/_/g, ' ');
	}

	// Format gender
	function formatGender(gender: string): string {
		return gender.charAt(0).toUpperCase() + gender.slice(1).toLowerCase();
	}
</script>

<svelte:head>
	<title>Profile | Fitbit Dashboard</title>
</svelte:head>

<div class="p-4 sm:p-6 lg:p-8">
	<div class="mb-6">
		<a href="/" class="text-fitbit-steps hover:underline text-sm">&larr; Back to Dashboard</a>
	</div>

	<h1 class="text-2xl font-bold text-white mb-8">Profile</h1>

	{#if $profile}
		<div class="max-w-2xl space-y-6">
			<!-- Profile Header Card -->
			<div class="bg-dark-card rounded-xl border border-dark-border p-6">
				<div class="flex items-center gap-6">
					{#if $profile.avatar}
						<img
							src="data:image/jpeg;base64,{$profile.avatar}"
							alt={$profile.displayName}
							class="w-24 h-24 rounded-full object-cover"
						/>
					{:else}
						<div class="w-24 h-24 rounded-full bg-fitbit-steps flex items-center justify-center text-white text-4xl font-medium">
							{$profile.firstName?.charAt(0) || '?'}
						</div>
					{/if}
					<div>
						<h2 class="text-2xl font-bold text-white">{$profile.fullName}</h2>
						<p class="text-gray-400">{$profile.displayName}</p>
						<p class="text-gray-500 text-sm mt-1">{$profile.emailAddress}</p>
					</div>
				</div>
			</div>

			<!-- Account Info -->
			<div class="bg-dark-card rounded-xl border border-dark-border p-6">
				<h3 class="text-lg font-semibold text-white mb-4">Account</h3>
				<div class="grid grid-cols-2 gap-4">
					<div>
						<p class="text-xs text-gray-500 uppercase tracking-wide">Member Since</p>
						<p class="text-white">{formatDate($profile.memberSince)}</p>
					</div>
					<div>
						<p class="text-xs text-gray-500 uppercase tracking-wide">Timezone</p>
						<p class="text-white">{formatTimezone($profile.timezone)}</p>
					</div>
				</div>
			</div>

			<!-- Personal Info -->
			<div class="bg-dark-card rounded-xl border border-dark-border p-6">
				<h3 class="text-lg font-semibold text-white mb-4">Personal</h3>
				<div class="grid grid-cols-2 md:grid-cols-4 gap-4">
					<div>
						<p class="text-xs text-gray-500 uppercase tracking-wide">Age</p>
						<p class="text-white text-lg font-medium">{calculateAge($profile.dateOfBirth)}</p>
					</div>
					<div>
						<p class="text-xs text-gray-500 uppercase tracking-wide">Gender</p>
						<p class="text-white text-lg font-medium">{formatGender($profile.gender)}</p>
					</div>
					<div>
						<p class="text-xs text-gray-500 uppercase tracking-wide">Height</p>
						<p class="text-white text-lg font-medium">{formatHeight($profile.height)}</p>
					</div>
					<div>
						<p class="text-xs text-gray-500 uppercase tracking-wide">Weight</p>
						<p class="text-white text-lg font-medium">{formatWeight($profile.weight)}</p>
					</div>
				</div>
			</div>

			<!-- Stride Lengths -->
			<div class="bg-dark-card rounded-xl border border-dark-border p-6">
				<h3 class="text-lg font-semibold text-white mb-4">Stride Length</h3>
				<div class="grid grid-cols-2 gap-4">
					<div class="bg-dark-bg rounded-lg p-4 text-center">
						<p class="text-xs text-gray-500 uppercase tracking-wide mb-2">Walking</p>
						<p class="text-2xl font-bold text-fitbit-steps">{formatStride($profile.strideLengthWalking)}</p>
					</div>
					<div class="bg-dark-bg rounded-lg p-4 text-center">
						<p class="text-xs text-gray-500 uppercase tracking-wide mb-2">Running</p>
						<p class="text-2xl font-bold text-fitbit-active">{formatStride($profile.strideLengthRunning)}</p>
					</div>
				</div>
			</div>

			<!-- Unit Preferences -->
			<div class="bg-dark-card rounded-xl border border-dark-border p-6">
				<h3 class="text-lg font-semibold text-white mb-4">Unit Preferences</h3>
				<div class="grid grid-cols-2 md:grid-cols-3 gap-4">
					<div>
						<p class="text-xs text-gray-500 uppercase tracking-wide">Distance</p>
						<p class="text-white">{$profile.distanceUnit}</p>
					</div>
					<div>
						<p class="text-xs text-gray-500 uppercase tracking-wide">Weight</p>
						<p class="text-white">{$profile.weightUnit}</p>
					</div>
					<div>
						<p class="text-xs text-gray-500 uppercase tracking-wide">Height</p>
						<p class="text-white">{$profile.heightUnit}</p>
					</div>
				</div>
			</div>

			<!-- App Settings -->
			<div class="bg-dark-card rounded-xl border border-dark-border p-6">
				<h3 class="text-lg font-semibold text-white mb-4">App Settings</h3>
				<div class="flex items-center justify-between">
					<div>
						<p class="text-white">Theme</p>
						<p class="text-sm text-gray-500">Toggle between dark and light mode</p>
					</div>
					<button
						onclick={handleThemeToggle}
						class="px-4 py-2 bg-dark-bg rounded-lg text-white hover:bg-dark-border transition-colors"
					>
						{$preferences.theme === 'dark' ? 'Dark' : 'Light'}
					</button>
				</div>
			</div>
		</div>
	{:else}
		<div class="bg-dark-card rounded-xl border border-dark-border p-8 text-center">
			<p class="text-gray-400">No profile data available</p>
		</div>
	{/if}
</div>
