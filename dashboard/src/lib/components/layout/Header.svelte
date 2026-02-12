<script lang="ts">
    import {formattedDate, goToNextDay, goToPreviousDay, goToToday, selectedDate, setDate} from '$stores/dashboard';
    import ProfileAvatar from './ProfileAvatar.svelte';
    import {
        addDays,
        endOfMonth,
        endOfWeek,
        format,
        isSameDay,
        isSameMonth,
        isToday,
        startOfMonth,
        startOfWeek
    } from 'date-fns';

    let showDatePicker = $state(false);
	let currentMonth = $state(new Date());

	function handleDateClick(date: Date) {
		setDate(date);
		showDatePicker = false;
	}

	function prevMonth() {
		currentMonth = new Date(currentMonth.getFullYear(), currentMonth.getMonth() - 1, 1);
	}

	function nextMonth() {
		currentMonth = new Date(currentMonth.getFullYear(), currentMonth.getMonth() + 1, 1);
	}

	function getCalendarDays(month: Date): Date[] {
		const start = startOfWeek(startOfMonth(month));
		const end = endOfWeek(endOfMonth(month));
		const days: Date[] = [];
		let current = start;
		while (current <= end) {
			days.push(current);
			current = addDays(current, 1);
		}
		return days;
	}

	let calendarDays = $derived(getCalendarDays(currentMonth));

	function handleClickOutside(event: MouseEvent) {
		const target = event.target as HTMLElement;
		if (!target.closest('.date-picker-container')) {
			showDatePicker = false;
		}
	}

	$effect(() => {
		if (showDatePicker) {
			// Sync calendar month with selected date when opening
			currentMonth = new Date($selectedDate.getFullYear(), $selectedDate.getMonth(), 1);
			document.addEventListener('click', handleClickOutside);
			return () => document.removeEventListener('click', handleClickOutside);
		}
	});
</script>

<header class="bg-theme-card border-b border-theme-border sticky top-0 z-50">
	<div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
		<div class="flex items-center justify-between h-16">
			<!-- Logo -->
			<div class="flex items-center">
				<a href="/" class="flex items-center space-x-2">
					<svg
						class="w-8 h-8 text-fitbit-steps"
						viewBox="0 0 24 24"
						fill="currentColor"
					>
						<circle cx="12" cy="4" r="2" />
						<circle cx="12" cy="10" r="2" />
						<circle cx="12" cy="16" r="2" />
						<circle cx="6" cy="7" r="2" />
						<circle cx="6" cy="13" r="2" />
						<circle cx="18" cy="7" r="2" />
						<circle cx="18" cy="13" r="2" />
					</svg>
					<span class="text-xl font-semibold text-theme-text">Fitbit</span>
				</a>
			</div>

			<!-- Date Selector -->
			<div class="flex items-center space-x-2">
				<button
					onclick={goToPreviousDay}
					class="p-2 rounded-lg hover:bg-theme-border transition-colors"
					aria-label="Previous day"
				>
					<svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
						<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
					</svg>
				</button>

				<div class="relative date-picker-container">
					<button
						onclick={(e) => { e.stopPropagation(); showDatePicker = !showDatePicker; }}
						class="px-4 py-2 rounded-lg hover:bg-theme-border transition-colors text-center min-w-[200px]"
					>
						<span class="text-sm text-theme-text-secondary">
							{#if isToday($selectedDate)}
								Today
							{:else}
								{format($selectedDate, 'MMM d')}
							{/if}
						</span>
						<p class="text-theme-text font-medium">{$formattedDate}</p>
					</button>

					<!-- Date Picker Dropdown -->
					{#if showDatePicker}
						<div class="absolute top-full left-1/2 -translate-x-1/2 mt-2 bg-theme-card border border-theme-border rounded-xl shadow-xl p-4 z-50 min-w-[300px]">
							<!-- Month Navigation -->
							<div class="flex items-center justify-between mb-4">
								<button
									onclick={prevMonth}
									class="p-1 rounded hover:bg-theme-border transition-colors"
									aria-label="Previous month"
								>
									<svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
										<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
									</svg>
								</button>
								<span class="text-theme-text font-medium">
									{format(currentMonth, 'MMMM yyyy')}
								</span>
								<button
									onclick={nextMonth}
									class="p-1 rounded hover:bg-theme-border transition-colors"
									aria-label="Next month"
								>
									<svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
										<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
									</svg>
								</button>
							</div>

							<!-- Day Headers -->
							<div class="grid grid-cols-7 gap-1 mb-2">
								{#each ['Su', 'Mo', 'Tu', 'We', 'Th', 'Fr', 'Sa'] as day}
									<div class="text-center text-xs text-theme-text-muted font-medium py-1">
										{day}
									</div>
								{/each}
							</div>

							<!-- Calendar Days -->
							<div class="grid grid-cols-7 gap-1">
								{#each calendarDays as day}
									<button
										onclick={() => handleDateClick(day)}
										class="p-2 text-sm rounded-lg transition-colors
											{isSameDay(day, $selectedDate) ? 'bg-fitbit-steps text-white' : ''}
											{!isSameMonth(day, currentMonth) ? 'text-theme-text-dim' : 'text-theme-text-bright'}
											{isToday(day) && !isSameDay(day, $selectedDate) ? 'ring-1 ring-fitbit-steps' : ''}
											hover:bg-theme-border"
									>
										{format(day, 'd')}
									</button>
								{/each}
							</div>

							<!-- Quick Actions -->
							<div class="mt-4 pt-3 border-t border-theme-border flex justify-between">
								<button
									onclick={() => handleDateClick(new Date())}
									class="text-sm text-fitbit-steps hover:underline"
								>
									Today
								</button>
								<button
									onclick={() => handleDateClick(addDays(new Date(), -1))}
									class="text-sm text-theme-text-secondary hover:text-theme-text"
								>
									Yesterday
								</button>
								<button
									onclick={() => handleDateClick(addDays(new Date(), -7))}
									class="text-sm text-theme-text-secondary hover:text-theme-text"
								>
									Last Week
								</button>
							</div>
						</div>
					{/if}
				</div>

				<button
					onclick={goToNextDay}
					class="p-2 rounded-lg hover:bg-theme-border transition-colors"
					aria-label="Next day"
				>
					<svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
						<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
					</svg>
				</button>

				<button
					onclick={goToToday}
					class="px-3 py-1 text-sm bg-fitbit-steps/20 text-fitbit-steps rounded-lg hover:bg-fitbit-steps/30 transition-colors"
				>
					Today
				</button>
			</div>

			<!-- Profile -->
			<div class="flex items-center">
				<ProfileAvatar />
			</div>
		</div>
	</div>
</header>
