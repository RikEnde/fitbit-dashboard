package kenny.fitbitkotlin.heartrate

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "heart_rates",
    indexes = [
        Index(name = "idx_heartrate_time", columnList = "time")
    ]
)
data class HeartRate(
    @Column(nullable = false)
    val bpm: Int,

    @Column(nullable = false)
    val confidence: Int,

    @Column(nullable = false)
    val time: LocalDateTime,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null
)

@Entity
@Table(
    name = "time_in_heart_rate_zones",
    indexes = [
        Index(name = "idx_time_in_heart_rate_zones_date_time", columnList = "dateTime")
    ]
)
data class TimeInHeartRateZones(
    @Column(nullable = false)
    val dateTime: LocalDateTime,

    @OneToMany(mappedBy = "timeInHeartRateZones", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    val zoneValues: MutableList<TimeInHeartRateZoneValue> = mutableListOf(),

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null
)

@Entity
@Table(
    name = "time_in_heart_rate_zone_values",
    indexes = [
        Index(name = "idx_time_in_heart_rate_zone_values_parent_id", columnList = "time_in_heart_rate_zones_id")
    ]
)
data class TimeInHeartRateZoneValue(
    @Column(nullable = false)
    val zoneName: String,

    @Column(nullable = false)
    val minutes: Double,

    @ManyToOne
    @JoinColumn(name = "time_in_heart_rate_zones_id", nullable = false)
    val timeInHeartRateZones: TimeInHeartRateZones,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null
)

@Entity
@Table(
    name = "resting_heart_rates",
    indexes = [
        Index(name = "idx_resting_heart_rate_date_time", columnList = "dateTime")
    ]
)
data class RestingHeartRate(
    @Column(nullable = false)
    val value: Double,

    @Column(nullable = false)
    val error: Double,

    @Column(nullable = false)
    val dateTime: LocalDateTime,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null
)

@Entity
@Table(
    name = "daily_heart_rate_variabilities",
    indexes = [
        Index(name = "idx_daily_heart_rate_variability_timestamp", columnList = "timestamp")
    ]
)
data class DailyHeartRateVariability(
    @Column(nullable = false)
    val timestamp: LocalDateTime,

    @Column(nullable = false)
    val rmssd: Double,

    @Column(nullable = false)
    val nremhr: Double,

    @Column(nullable = false)
    val entropy: Double,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null
)

@Entity
@Table(
    name = "heart_rate_variability_details",
    indexes = [
        Index(name = "idx_heart_rate_variability_details_timestamp", columnList = "timestamp")
    ]
)
data class HeartRateVariabilityDetails(
    @Column(nullable = false)
    val timestamp: LocalDateTime,

    @Column(nullable = false)
    val rmssd: Double,

    @Column(nullable = false)
    val coverage: Double,

    @Column(nullable = false)
    val lowFrequency: Double,

    @Column(nullable = false)
    val highFrequency: Double,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null
)
