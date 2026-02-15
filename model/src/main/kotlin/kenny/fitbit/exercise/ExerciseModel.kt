package kenny.fitbit.exercise

import jakarta.persistence.*
import kenny.fitbit.profile.Profile
import java.time.LocalDateTime

@Entity
@Table(
    name = "exercise",
    indexes = [
        Index(name = "idx_exercise_start_time", columnList = "startTime"),
        Index(name = "idx_exercise_log_id", columnList = "logId")
    ]
)
data class Exercise(
    @Column(nullable = false)
    val logId: Long,

    @Column(nullable = false)
    val activityName: String,

    @Column(nullable = false)
    val activityTypeId: Int,

    @Column(nullable = true)
    val averageHeartRate: Int?,

    @Column(nullable = false)
    val calories: Int,

    @Column(nullable = false)
    val duration: Long,

    @Column(nullable = false)
    val activeDuration: Long,

    @Column(nullable = true)
    val steps: Int?,

    @Column(nullable = false)
    val logType: String,

    @Column(nullable = false)
    val startTime: LocalDateTime,

    @Column(nullable = true)
    val lastModified: LocalDateTime?,

    @Column(nullable = true)
    val originalStartTime: LocalDateTime?,

    @Column(nullable = true)
    val originalDuration: Long?,

    @Column(nullable = true)
    val elevationGain: Double?,

    @Column(nullable = false)
    val hasGps: Boolean = false,

    @Column(nullable = false)
    val shouldFetchDetails: Boolean = false,

    @Column(nullable = false)
    val hasActiveZoneMinutes: Boolean = false,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    val profile: Profile,

    @OneToMany(mappedBy = "exercise", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    val heartRateZones: MutableList<HeartRateZone> = mutableListOf(),

    @OneToMany(mappedBy = "exercise", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    val activityLevels: MutableList<ActivityLevel> = mutableListOf(),

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null
)

@Entity
@Table(name = "heart_rate_zone")
data class HeartRateZone(
    @Column(nullable = false)
    val name: String,

    @Column(nullable = false)
    val min: Int,

    @Column(nullable = false)
    val max: Int,

    @Column(nullable = false)
    val minutes: Int,

    @ManyToOne
    @JoinColumn(name = "exercise_id", nullable = false)
    val exercise: Exercise,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null
)

@Entity
@Table(name = "activity_level")
data class ActivityLevel(
    @Column(nullable = false)
    val name: String,

    @Column(nullable = false)
    val minutes: Int,

    @ManyToOne
    @JoinColumn(name = "exercise_id", nullable = false)
    val exercise: Exercise,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null
)

@Entity
@Table(
    name = "activity_minutes",
    indexes = [
        Index(name = "idx_activity_minutes_date_time", columnList = "dateTime")
    ]
)
data class ActivityMinutes(
    @Column(nullable = false)
    val dateTime: LocalDateTime,

    @Column(nullable = false)
    val value: Int,

    @Column(nullable = false)
    val intensity: String,  // "sedentary", "light", "moderate", "active"

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    val profile: Profile,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null
)

@Entity
@Table(
    name = "demographic_vo2_max",
    indexes = [
        Index(name = "idx_demographic_vo2_max_date_time", columnList = "dateTime")
    ]
)
data class DemographicVO2Max(
    @Column(nullable = false)
    val demographicVO2Max: Double,

    @Column(nullable = false)
    val demographicVO2MaxError: Double,

    @Column(nullable = false)
    val filteredDemographicVO2Max: Double,

    @Column(nullable = false)
    val filteredDemographicVO2MaxError: Double,

    @Column(nullable = false)
    val dateTime: LocalDateTime,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    val profile: Profile,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null
)

@Entity
@Table(
    name = "run_vo2_max",
    indexes = [
        Index(name = "idx_run_vo2_max_date_time", columnList = "dateTime"),
        Index(name = "idx_run_vo2_max_exercise_id", columnList = "exerciseId")
    ]
)
data class RunVO2Max(
    @Column(nullable = false)
    val exerciseId: Long,

    @Column(nullable = false)
    val runVO2Max: Double,

    @Column(nullable = false)
    val runVO2MaxError: Double,

    @Column(nullable = false)
    val filteredRunVO2Max: Double,

    @Column(nullable = false)
    val filteredRunVO2MaxError: Double,

    @Column(nullable = false)
    val dateTime: LocalDateTime,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    val profile: Profile,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null
)

@Entity
@Table(
    name = "activity_goals",
    indexes = [
        Index(name = "idx_activity_goal_created_on", columnList = "createdOn")
    ]
)
data class ActivityGoal(
    @Column(nullable = false)
    val type: String,

    @Column(nullable = false)
    val frequency: String,

    @Column(nullable = false)
    val target: Double,

    @Column(nullable = true)
    val result: Double?,

    @Column(nullable = false)
    val status: String,

    @Column(nullable = false)
    val isPrimary: Boolean,

    @Column(nullable = true)
    val startDate: LocalDateTime?,

    @Column(nullable = true)
    val endDate: LocalDateTime?,

    @Column(nullable = false)
    val createdOn: LocalDateTime,

    @Column(nullable = true)
    val editedOn: LocalDateTime?,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    val profile: Profile,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null
)

