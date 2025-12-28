// Fitbit color palette
export const colors = {
	steps: '#00B0B9',
	calories: '#FF6B35',
	distance: '#7C3AED',
	active: '#22C55E',
	heartrate: '#EF4444',
	sleep: '#6366F1'
} as const;

// Sleep stage colors
export const sleepStageColors = {
	awake: '#EF4444',
	rem: '#8B5CF6',
	light: '#60A5FA',
	deep: '#3B82F6'
} as const;

// Heart rate zone colors
export const heartRateZoneColors = {
	'Out of Range': '#9CA3AF',
	'Fat Burn': '#FCD34D',
	Cardio: '#FB923C',
	Peak: '#EF4444'
} as const;

export type MetricType = keyof typeof colors;
