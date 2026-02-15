package kenny.fitbit.exercise

import kenny.fitbit.heartrate.TimeInHeartRateZones
import kenny.fitbit.profile.Profile
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface ExerciseRepository :
    JpaRepository<Exercise, Long>,
    JpaSpecificationExecutor<Exercise> {

    fun findByProfile(profile: Profile, pageable: Pageable): Page<Exercise>

    fun findByProfileAndStartTimeBetween(
        profile: Profile,
        from: LocalDateTime,
        to: LocalDateTime,
        pageable: Pageable
    ): Page<Exercise>
}

@Repository
interface HeartRateZoneRepository :
    JpaRepository<HeartRateZone, Long>

@Repository
interface ActivityLevelRepository :
    JpaRepository<ActivityLevel, Long>

@Repository
interface ActivityMinutesRepository : JpaRepository<ActivityMinutes, Long>, JpaSpecificationExecutor<ActivityMinutes> {
    fun findByProfileAndDateTimeBetween(profile: Profile, from: LocalDateTime, to: LocalDateTime, pageable: Pageable): Page<ActivityMinutes>
}

@Repository
interface RunVO2MaxRepository : JpaRepository<RunVO2Max, Long>, JpaSpecificationExecutor<RunVO2Max> {
    fun findByProfileAndDateTimeBetween(profile: Profile, from: LocalDateTime, to: LocalDateTime, pageable: Pageable): Page<RunVO2Max>
}

@Repository
interface ActivityGoalRepository : JpaRepository<ActivityGoal, Long>, JpaSpecificationExecutor<ActivityGoal> {
    fun findByProfileAndStartDateBetween(profile: Profile, from: LocalDateTime, to: LocalDateTime, pageable: Pageable): Page<ActivityGoal>
}

@Repository
interface DemographicVO2MaxRepository : JpaRepository<DemographicVO2Max, Long>,
    JpaSpecificationExecutor<DemographicVO2Max> {
    fun findByProfileAndDateTimeBetween(profile: Profile, from: LocalDateTime, to: LocalDateTime, pageable: Pageable): Page<DemographicVO2Max>
}

@Repository
interface TimeInHeartRateZonesRepository : JpaRepository<TimeInHeartRateZones, Long>,
    JpaSpecificationExecutor<TimeInHeartRateZones> {
    fun findByProfileAndDateTimeBetween(profile: Profile, from: LocalDateTime, to: LocalDateTime, pageable: Pageable): Page<TimeInHeartRateZones>
}
