import {derived, writable} from 'svelte/store';
import {endOfDay, format, startOfDay} from 'date-fns';

// Selected date for the dashboard
export const selectedDate = writable<Date>(new Date());

// Format a date as RFC3339 string with local timezone offset
// This preserves local time which matches how data is stored in the database
export function toLocalISOString(date: Date): string {
	const pad = (n: number) => n.toString().padStart(2, '0');
	const offsetMinutes = date.getTimezoneOffset();
	const offsetSign = offsetMinutes <= 0 ? '+' : '-';
	const offsetHours = Math.floor(Math.abs(offsetMinutes) / 60);
	const offsetMins = Math.abs(offsetMinutes) % 60;
	const offset = `${offsetSign}${pad(offsetHours)}:${pad(offsetMins)}`;
	return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}${offset}`;
}

// Derived store for date range (start and end of selected day)
export const dateRange = derived(selectedDate, ($date) => ({
	from: toLocalISOString(startOfDay($date)),
	to: toLocalISOString(endOfDay($date))
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
