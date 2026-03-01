import {gql} from '@urql/svelte';

// Profile query for the authenticated user
export const PROFILE_QUERY = gql`
	query Profile {
		profile {
			id
			fullName
			displayName
			firstName
			lastName
			emailAddress
			dateOfBirth
			memberSince
			gender
			height
			weight
			strideLengthWalking
			strideLengthRunning
			weightUnit
			distanceUnit
			heightUnit
			timezone
			avatar
		}
	}
`;

// Latest date with data
export const LATEST_DATA_DATE_QUERY = gql`
	query LatestDataDate {
		latestDataDate
	}
`;

// Heart rates with optional resting heart rate
export const HEART_RATE_QUERY = gql`
	query HeartRates($limit: Int, $offset: Int, $range: DateRange, $date: Date!) {
		heartRates(limit: $limit, offset: $offset, range: $range) {
			id
			bpm
			confidence
			dateTime
		}
		restingHeartRate(date: $date) {
			value
		}
	}
`;

// Resting heart rate trend
export const RESTING_HEART_RATES_QUERY = gql`
	query RestingHeartRates($limit: Int, $range: DateRange) {
		restingHeartRates(limit: $limit, range: $range) {
			value
			dateTime
		}
	}
`;

// Raw steps data
export const STEPS_QUERY = gql`
	query Steps($limit: Int, $range: DateRange) {
		steps(limit: $limit, range: $range) {
			id
			value
			dateTime
		}
	}
`;

// Daily steps summary
export const DAILY_STEPS_SUM_QUERY = gql`
	query DailyStepsSum($range: DateRange!) {
		dailyStepsSum(range: $range) {
			date
			totalSteps
		}
	}
`;

// Weekly steps average
export const WEEKLY_STEPS_AVERAGE_QUERY = gql`
	query WeeklyStepsAverage($range: DateRange!) {
		weeklyStepsAverage(range: $range) {
			weekNumber
			averageSteps
		}
	}
`;

// Sleep data
export const SLEEPS_QUERY = gql`
	query Sleeps($limit: Int, $offset: Int, $range: DateRange) {
		sleeps(limit: $limit, offset: $offset, range: $range) {
			id
			logId
			dateOfSleep
			startTime
			endTime
			duration
			minutesToFallAsleep
			minutesAsleep
			minutesAwake
			minutesAfterWakeup
			timeInBed
			efficiency
			mainSleep
			levelSummaries {
				level
				count
				minutes
				thirtyDayAvgMinutes
			}
			levelData {
				dateTime
				level
				seconds
			}
		}
	}
`;

// Sleep scores
export const SLEEP_SCORES_QUERY = gql`
	query SleepScores($limit: Int, $offset: Int, $range: DateRange) {
		sleepScores(limit: $limit, offset: $offset, range: $range) {
			id
			sleepLogEntryId
			timestamp
			overallScore
			compositionScore
			revitalizationScore
			durationScore
			deepSleepInMinutes
			restingHeartRate
			restlessness
		}
	}
`;

// Calories data
export const CALORIES_QUERY = gql`
	query Calories($limit: Int, $offset: Int, $range: DateRange) {
		calories(limit: $limit, offset: $offset, range: $range) {
			id
			value
			dateTime
		}
	}
`;

// Distance data
export const DISTANCES_QUERY = gql`
	query Distances($limit: Int, $offset: Int, $range: DateRange) {
		distances(limit: $limit, offset: $offset, range: $range) {
			id
			value
			dateTime
		}
	}
`;

// Exercises
export const EXERCISES_QUERY = gql`
	query Exercises($limit: Int, $offset: Int, $range: DateRange) {
		exercises(limit: $limit, offset: $offset, range: $range) {
			id
			logId
			activityName
			activityTypeId
			averageHeartRate
			calories
			duration
			activeDuration
			steps
			startTime
			elevationGain
			hasGps
			heartRateZones {
				name
				min
				max
				minutes
			}
			activityLevels {
				name
				minutes
			}
		}
	}
`;
