import { writable, derived } from 'svelte/store';
import { startOfDay, endOfDay, format } from 'date-fns';

// Selected date for the dashboard
export const selectedDate = writable<Date>(new Date());

// Derived store for date range (start and end of selected day)
export const dateRange = derived(selectedDate, ($date) => ({
	from: startOfDay($date).toISOString(),
	to: endOfDay($date).toISOString()
}));

// Formatted date for display
export const formattedDate = derived(selectedDate, ($date) => format($date, 'EEEE, MMMM d, yyyy'));

// Navigation helpers
export function goToToday() {
	selectedDate.set(new Date());
}

export function goToPreviousDay() {
	selectedDate.update((date) => {
		const newDate = new Date(date);
		newDate.setDate(newDate.getDate() - 1);
		return newDate;
	});
}

export function goToNextDay() {
	selectedDate.update((date) => {
		const newDate = new Date(date);
		newDate.setDate(newDate.getDate() + 1);
		return newDate;
	});
}

export function setDate(date: Date) {
	selectedDate.set(date);
}
