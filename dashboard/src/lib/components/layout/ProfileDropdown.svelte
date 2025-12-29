<script lang="ts">
	import { profile } from '$stores/profile';
	import { preferences } from '$stores/preferences';
	import {
		formatHeight,
		formatWeight,
		formatStride,
		calculateAge,
		formatDate
	} from '$utils/formatters';

	interface Props {
		onClose: () => void;
	}

	let { onClose }: Props = $props();

	function handleThemeToggle() {
		preferences.toggleTheme();
	}
</script>

<div
	class="absolute right-0 mt-2 w-80 bg-dark-card border border-dark-border rounded-xl shadow-xl overflow-hidden z-50"
	role="menu"
>
	{#if $profile}
		<!-- Profile Header -->
		<div class="p-4 border-b border-dark-border">
			<div class="flex items-center space-x-3">
				{#if $profile.avatar}
					<img
						src="data:image/jpeg;base64,{$profile.avatar}"
						alt={$profile.displayName}
						class="w-16 h-16 rounded-full object-cover"
					/>
				{:else}
					<div class="w-16 h-16 rounded-full bg-fitbit-steps flex items-center justify-center text-white text-2xl font-medium">
						{$profile.firstName?.charAt(0) || '?'}
					</div>
				{/if}
				<div>
					<p class="text-white font-semibold">{$profile.displayName || $profile.fullName}</p>
					<p class="text-gray-400 text-sm">{$profile.emailAddress}</p>
				</div>
			</div>
		</div>

		<!-- Member Info -->
		<div class="p-4 border-b border-dark-border text-sm">
			<div class="flex justify-between text-gray-400 mb-2">
				<span>Member since</span>
				<span class="text-white">{formatDate($profile.memberSince)}</span>
			</div>
			<div class="flex justify-between text-gray-400 mb-2">
				<span>Age</span>
				<span class="text-white">{calculateAge($profile.dateOfBirth)} years</span>
			</div>
			<div class="flex justify-between text-gray-400 mb-2">
				<span>Height</span>
				<span class="text-white">{formatHeight($profile.height)}</span>
			</div>
			<div class="flex justify-between text-gray-400">
				<span>Weight</span>
				<span class="text-white">{formatWeight($profile.weight)}</span>
			</div>
		</div>

		<!-- Stride Info -->
		<div class="p-4 border-b border-dark-border text-sm">
			<p class="text-gray-400 mb-2">Stride Length</p>
			<div class="flex justify-between text-gray-400 mb-1">
				<span>Walking</span>
				<span class="text-white">{formatStride($profile.strideLengthWalking)}</span>
			</div>
			<div class="flex justify-between text-gray-400">
				<span>Running</span>
				<span class="text-white">{formatStride($profile.strideLengthRunning)}</span>
			</div>
		</div>

		<!-- Actions -->
		<div class="p-2 space-y-1">
			<a
				href="/profile"
				onclick={onClose}
				class="w-full flex items-center px-3 py-2 rounded-lg hover:bg-dark-border transition-colors text-gray-300"
			>
				<svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
					<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
				</svg>
				View Profile
			</a>
			<button
				onclick={handleThemeToggle}
				class="w-full flex items-center justify-between px-3 py-2 rounded-lg hover:bg-dark-border transition-colors text-left"
			>
				<span class="text-gray-300 flex items-center">
					<svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
						<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20.354 15.354A9 9 0 018.646 3.646 9.003 9.003 0 0012 21a9.003 9.003 0 008.354-5.646z" />
					</svg>
					Theme
				</span>
				<span class="text-sm text-gray-400">
					{$preferences.theme === 'dark' ? 'Dark' : 'Light'}
				</span>
			</button>
		</div>
	{:else}
		<div class="p-8 text-center text-gray-400">
			<p>No profile loaded</p>
		</div>
	{/if}
</div>
