package kenny.fitbit.sleep

import kenny.fitbit.profile.Profile
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface SleepRepository :
    JpaRepository<Sleep, Long>,
    JpaSpecificationExecutor<Sleep> {

    @Query("SELECT s.logId FROM Sleep s WHERE s.profile = :profile")
    fun findAllLogIdsByProfile(profile: Profile): Set<Long>

    fun findByProfile(profile: Profile, pageable: Pageable): Page<Sleep>

    fun findByProfileAndStartTimeBetween(
        profile: Profile,
        from: LocalDateTime,
        to: LocalDateTime,
        pageable: Pageable
    ): Page<Sleep>
}

@Repository
interface SleepScoreRepository :
    JpaRepository<SleepScore, Long>,
    JpaSpecificationExecutor<SleepScore> {

    fun findByProfile(profile: Profile, pageable: Pageable): Page<SleepScore>

    fun findByProfileAndTimestampBetween(
        profile: Profile,
        from: LocalDateTime,
        to: LocalDateTime,
        pageable: Pageable
    ): Page<SleepScore>
}

@Repository
interface SleepLevelSummaryRepository :
    JpaRepository<SleepLevelSummary, Long>

@Repository
interface SleepLevelDataRepository :
    JpaRepository<SleepLevelData, Long>

@Repository
interface SleepLevelShortDataRepository :
    JpaRepository<SleepLevelShortData, Long>

@Repository
interface DeviceTemperatureRepository : JpaRepository<DeviceTemperature, Long>, JpaSpecificationExecutor<DeviceTemperature> {
    fun findByProfile(profile: Profile, pageable: Pageable): Page<DeviceTemperature>
    fun findByProfileAndRecordedTimeBetween(profile: Profile, from: LocalDateTime, to: LocalDateTime, pageable: Pageable): Page<DeviceTemperature>
}

@Repository
interface DailyRespiratoryRateRepository : JpaRepository<DailyRespiratoryRate, Long>, JpaSpecificationExecutor<DailyRespiratoryRate> {
    fun findByProfile(profile: Profile, pageable: Pageable): Page<DailyRespiratoryRate>
    fun findByProfileAndTimestampBetween(profile: Profile, from: LocalDateTime, to: LocalDateTime, pageable: Pageable): Page<DailyRespiratoryRate>
}

@Repository
interface MinuteSpO2Repository : JpaRepository<MinuteSpO2, Long>, JpaSpecificationExecutor<MinuteSpO2> {
    fun findByProfile(profile: Profile, pageable: Pageable): Page<MinuteSpO2>
    fun findByProfileAndTimestampBetween(profile: Profile, from: LocalDateTime, to: LocalDateTime, pageable: Pageable): Page<MinuteSpO2>
}

@Repository
interface ComputedTemperatureRepository : JpaRepository<ComputedTemperature, Long>, JpaSpecificationExecutor<ComputedTemperature> {
    fun findByProfile(profile: Profile, pageable: Pageable): Page<ComputedTemperature>
    fun findByProfileAndSleepStartBetween(profile: Profile, from: LocalDateTime, to: LocalDateTime, pageable: Pageable): Page<ComputedTemperature>
}

@Repository
interface RespiratoryRateSummaryRepository : JpaRepository<RespiratoryRateSummary, Long>, JpaSpecificationExecutor<RespiratoryRateSummary> {
    fun findByProfile(profile: Profile, pageable: Pageable): Page<RespiratoryRateSummary>
    fun findByProfileAndTimestampBetween(profile: Profile, from: LocalDateTime, to: LocalDateTime, pageable: Pageable): Page<RespiratoryRateSummary>
}

@Repository
interface DailySpO2Repository : JpaRepository<DailySpO2, Long>, JpaSpecificationExecutor<DailySpO2> {
    fun findByProfile(profile: Profile, pageable: Pageable): Page<DailySpO2>
    fun findByProfileAndTimestampBetween(profile: Profile, from: LocalDateTime, to: LocalDateTime, pageable: Pageable): Page<DailySpO2>
}
