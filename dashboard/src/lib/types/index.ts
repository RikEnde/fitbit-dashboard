// GraphQL response types

export interface DateRange {
	from: string;
	to: string;
}

export interface DailyStepsSum {
	date: string;
	totalSteps: number;
}

export interface HeartRate {
	id: string;
	bpm: number;
	confidence: number;
	time: string;
}

export interface SleepLevelSummary {
	level: string;
	count: number;
	minutes: number;
	thirtyDayAvgMinutes: number;
}

export interface Sleep {
	logId: string;
	dateOfSleep: string;
	startTime: string;
	endTime: string;
	duration: number;
	minutesToFallAsleep: number;
	minutesAsleep: number;
	minutesAwake: number;
	efficiency: number;
	mainSleep: boolean;
	levelSummaries: SleepLevelSummary[];
}

export interface SleepScore {
	id: string;
	sleepLogEntryId: string;
	timestamp: string;
	overallScore: number;
	compositionScore: number | null;
	revitalizationScore: number;
	durationScore: number | null;
	deepSleepInMinutes: number;
	restingHeartRate: number;
	restlessness: number;
}

export interface Calories {
	id: string;
	value: number;
	dateTime: string;
}

export interface Distance {
	id: string;
	value: number;
	dateTime: string;
}

export interface HeartRateZone {
	name: string;
	min: number;
	max: number;
	minutes: number;
}

export interface ActivityLevel {
	name: string;
	minutes: number;
}

export interface Exercise {
	logId: string;
	activityName: string;
	calories: number;
	duration: number;
	activeDuration: number;
	steps: number | null;
	startTime: string;
	averageHeartRate: number | null;
	hasGps: boolean;
	heartRateZones: HeartRateZone[];
	activityLevels: ActivityLevel[];
}
