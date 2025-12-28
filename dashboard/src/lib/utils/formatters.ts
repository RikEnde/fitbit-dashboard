import { format, formatDistanceToNow, differenceInYears, parseISO } from 'date-fns';

// Format numbers with commas
export function formatNumber(num: number): string {
	return num.toLocaleString();
}

// Format duration in minutes to hours and minutes
export function formatDuration(minutes: number): string {
	const hours = Math.floor(minutes / 60);
	const mins = minutes % 60;
	if (hours === 0) return `${mins}m`;
	if (mins === 0) return `${hours}h`;
	return `${hours}h ${mins}m`;
}

// Format duration in milliseconds to hours and minutes
export function formatDurationMs(ms: number): string {
	return formatDuration(Math.round(ms / 60000));
}

// Format distance (assumes meters input)
export function formatDistance(meters: number, unit: 'mi' | 'km' = 'mi'): string {
	if (unit === 'mi') {
		const miles = meters / 1609.34;
		return `${miles.toFixed(2)} mi`;
	}
	const km = meters / 1000;
	return `${km.toFixed(2)} km`;
}

// Format height
export function formatHeight(cm: number, unit: 'in' | 'cm' = 'in'): string {
	if (unit === 'in') {
		const totalInches = cm / 2.54;
		const feet = Math.floor(totalInches / 12);
		const inches = Math.round(totalInches % 12);
		return `${feet}'${inches}"`;
	}
	return `${cm} cm`;
}

// Format weight
export function formatWeight(kg: number, unit: 'lb' | 'kg' = 'lb'): string {
	if (unit === 'lb') {
		const lbs = kg * 2.20462;
		return `${lbs.toFixed(1)} lbs`;
	}
	return `${kg.toFixed(1)} kg`;
}

// Format stride length (assumes meters input)
export function formatStride(meters: number): string {
	const feet = meters * 3.28084;
	return `${feet.toFixed(1)} ft`;
}

// Calculate age from date of birth
export function calculateAge(dateOfBirth: string): number {
	return differenceInYears(new Date(), parseISO(dateOfBirth));
}

// Format date for display
export function formatDate(date: string | Date): string {
	const d = typeof date === 'string' ? parseISO(date) : date;
	return format(d, 'MMM d, yyyy');
}

// Format time for display
export function formatTime(date: string | Date): string {
	const d = typeof date === 'string' ? parseISO(date) : date;
	return format(d, 'h:mm a');
}

// Format relative time
export function formatRelative(date: string | Date): string {
	const d = typeof date === 'string' ? parseISO(date) : date;
	return formatDistanceToNow(d, { addSuffix: true });
}

// Format percentage
export function formatPercentage(value: number, total: number): string {
	if (total === 0) return '0%';
	return `${Math.round((value / total) * 100)}%`;
}

// Format calories
export function formatCalories(calories: number): string {
	return `${formatNumber(Math.round(calories))} cal`;
}
