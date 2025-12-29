import {gql} from '@urql/svelte';

// Profile query for the avatar dropdown
export const PROFILE_QUERY = gql`
	query Profiles {
		profiles(limit: 1) {
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

// Daily steps summary
export const DAILY_STEPS_SUM_QUERY = gql`
	query DailyStepsSum($range: DateRange!) {
		dailyStepsSum(range: $range) {
			date
			totalSteps
		}
	}
`;

// Heart rates for a day
export const HEART_RATES_QUERY = gql`
	query HeartRates($limit: Int, $offset: Int, $range: DateRange) {
		heartRates(limit: $limit, offset: $offset, range: $range) {
			id
			bpm
			confidence
			time
		}
	}
`;

// Sleep data
export const SLEEPS_QUERY = gql`
	query Sleeps($limit: Int, $offset: Int, $range: DateRange) {
		sleeps(limit: $limit, offset: $offset, range: $range) {
			logId
			dateOfSleep
			startTime
			endTime
			duration
			minutesToFallAsleep
			minutesAsleep
			minutesAwake
			efficiency
			mainSleep
			levelSummaries {
				level
				count
				minutes
				thirtyDayAvgMinutes
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
			logId
			activityName
			calories
			duration
			activeDuration
			steps
			startTime
			averageHeartRate
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
