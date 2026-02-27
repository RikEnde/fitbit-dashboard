package kenny.fitbit.sleep

import jakarta.persistence.*
import kenny.fitbit.profile.Profile
import org.hibernate.annotations.BatchSize
import java.time.LocalDateTime

@Entity
@Table(
    name = "sleep",
    indexes = [
        Index(name = "idx_sleep_start_time", columnList = "startTime"),
        Index(name = "idx_sleep_log_id", columnList = "logId", unique = true)
    ]
)
data class Sleep(
    @Column(nullable = false, unique = true)
    val logId: Long,

    @Column(nullable = false)
    val dateOfSleep: LocalDateTime,

    @Column(nullable = false)
    val startTime: LocalDateTime,

    @Column(nullable = false)
    val endTime: LocalDateTime,

    @Column(nullable = false)
    val duration: Long,

    @Column(nullable = false)
    val minutesToFallAsleep: Int,

    @Column(nullable = false)
    val minutesAsleep: Int,

    @Column(nullable = false)
    val minutesAwake: Int,

    @Column(nullable = false)
    val minutesAfterWakeup: Int,

    @Column(nullable = false)
    val timeInBed: Int,

    @Column(nullable = false)
    val efficiency: Int,

    @Column(nullable = false)
    val type: String,

    @Column(nullable = false)
    val infoCode: Int,

    @Column(nullable = false)
    val logType: String,

    @Column(nullable = false)
    val mainSleep: Boolean,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    val profile: Profile,

    @OneToMany(mappedBy = "sleep", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    @BatchSize(size = 50)
    val levelSummaries: MutableList<SleepLevelSummary> = mutableListOf(),

    @OneToMany(mappedBy = "sleep", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    @BatchSize(size = 50)
    val levelData: MutableList<SleepLevelData> = mutableListOf(),

    @OneToMany(mappedBy = "sleep", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    @BatchSize(size = 50)
    val levelShortData: MutableList<SleepLevelShortData> = mutableListOf(),

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null
)

@Entity
@Table(name = "sleep_level_summary")
data class SleepLevelSummary(
    @Column(nullable = false)
    val level: String,

    @Column(nullable = false)
    val count: Int,

    @Column(nullable = false)
    val minutes: Int,

    @Column(nullable = false)
    val thirtyDayAvgMinutes: Int,

    @ManyToOne
    @JoinColumn(name = "sleep_id", nullable = false)
    val sleep: Sleep,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null
)

@Entity
@Table(name = "sleep_level_data")
data class SleepLevelData(
    @Column(nullable = false)
    val dateTime: LocalDateTime,

    @Column(nullable = false)
    val level: String,

    @Column(nullable = false)
    val seconds: Int,

    @ManyToOne
    @JoinColumn(name = "sleep_id", nullable = false)
    val sleep: Sleep,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null
)

@Entity
@Table(name = "sleep_level_short_data")
data class SleepLevelShortData(
    @Column(nullable = false)
    val dateTime: LocalDateTime,

    @Column(nullable = false)
    val level: String,

    @Column(nullable = false)
    val seconds: Int,

    @ManyToOne
    @JoinColumn(name = "sleep_id", nullable = false)
    val sleep: Sleep,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null
)

@Entity
@Table(
    name = "sleep_score",
    indexes = [
        Index(name = "idx_sleep_score_timestamp", columnList = "timestamp"),
        Index(name = "idx_sleep_score_sleep_log_entry_id", columnList = "sleepLogEntryId", unique = true)
    ]
)
data class SleepScore(
    @Column(nullable = false, unique = true)
    val sleepLogEntryId: Long,

    @Column(nullable = false)
    val timestamp: LocalDateTime,

    @Column(nullable = false)
    val overallScore: Int,

    @Column(nullable = true)
    val compositionScore: Int?,

    @Column(nullable = false)
    val revitalizationScore: Int,

    @Column(nullable = true)
    val durationScore: Int?,

    @Column(nullable = false)
    val deepSleepInMinutes: Int,

    @Column(nullable = false)
    val restingHeartRate: Int,

    @Column(nullable = false)
    val restlessness: Double,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    val profile: Profile,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null
)

@Entity
@Table(
    name = "device_temperatures",
    indexes = [
        Index(name = "idx_device_temperature_recorded_time", columnList = "recordedTime")
    ]
)
data class DeviceTemperature(
    @Column(nullable = false)
    val recordedTime: LocalDateTime,

    @Column(nullable = false)
    val temperature: Double,

    @Column(nullable = false)
    val sensorType: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    val profile: Profile,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null
)

@Entity
@Table(
    name = "daily_respiratory_rates",
    indexes = [
        Index(name = "idx_daily_respiratory_rate_timestamp", columnList = "timestamp")
    ]
)
data class DailyRespiratoryRate(
    @Column(nullable = false)
    val timestamp: LocalDateTime,

    @Column(nullable = false)
    val dailyRespiratoryRate: Double,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    val profile: Profile,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null
)

@Entity
@Table(
    name = "minute_spo2",
    indexes = [
        Index(name = "idx_minute_spo2_timestamp", columnList = "timestamp")
    ]
)
data class MinuteSpO2(
    @Column(nullable = false)
    val timestamp: LocalDateTime,

    @Column(nullable = false)
    val value: Double,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    val profile: Profile,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null
)

@Entity
@Table(
    name = "computed_temperatures",
    indexes = [
        Index(name = "idx_computed_temperature_sleep_start", columnList = "sleepStart"),
        Index(name = "idx_computed_temperature_sleep_end", columnList = "sleepEnd")
    ]
)
data class ComputedTemperature(
    @Column(nullable = false)
    val type: String,

    @Column(nullable = false)
    val sleepStart: LocalDateTime,

    @Column(nullable = false)
    val sleepEnd: LocalDateTime,

    @Column(nullable = false)
    val temperatureSamples: Int,

    @Column(nullable = false)
    val nightlyTemperature: Double,

    @Column(nullable = false)
    val baselineRelativeSampleSum: Double,

    @Column(nullable = false)
    val baselineRelativeSampleSumOfSquares: Double,

    @Column(nullable = false)
    val baselineRelativeNightlyStandardDeviation: Double,

    @Column(nullable = false)
    val baselineRelativeSampleStandardDeviation: Double,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    val profile: Profile,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null
)

@Entity
@Table(
    name = "respiratory_rate_summaries",
    indexes = [
        Index(name = "idx_respiratory_rate_summary_timestamp", columnList = "timestamp")
    ]
)
data class RespiratoryRateSummary(
    @Column(nullable = false)
    val timestamp: LocalDateTime,

    @Column(nullable = false)
    val fullSleepBreathingRate: Double,

    @Column(nullable = false)
    val fullSleepStandardDeviation: Double,

    @Column(nullable = false)
    val fullSleepSignalToNoise: Double,

    @Column(nullable = false)
    val deepSleepBreathingRate: Double,

    @Column(nullable = false)
    val deepSleepStandardDeviation: Double,

    @Column(nullable = false)
    val deepSleepSignalToNoise: Double,

    @Column(nullable = false)
    val lightSleepBreathingRate: Double,

    @Column(nullable = false)
    val lightSleepStandardDeviation: Double,

    @Column(nullable = false)
    val lightSleepSignalToNoise: Double,

    @Column(nullable = true)
    val remSleepBreathingRate: Double,

    @Column(nullable = true)
    val remSleepStandardDeviation: Double,

    @Column(nullable = true)
    val remSleepSignalToNoise: Double,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    val profile: Profile,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null
)

@Entity
@Table(
    name = "daily_spo2",
    indexes = [
        Index(name = "idx_daily_spo2_timestamp", columnList = "timestamp")
    ]
)
data class DailySpO2(
    @Column(nullable = false)
    val timestamp: LocalDateTime,

    @Column(nullable = false)
    val averageValue: Double,

    @Column(nullable = false)
    val lowerBound: Double,

    @Column(nullable = false)
    val upperBound: Double,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    val profile: Profile,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null
)
