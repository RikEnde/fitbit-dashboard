package kenny.fitbit.sleep

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface SleepRepository :
    JpaRepository<Sleep, Long>,
    JpaSpecificationExecutor<Sleep> {

    fun findByLogId(logId: Long): Sleep?

    @org.springframework.data.jpa.repository.Query("SELECT s.logId FROM Sleep s")
    fun findAllLogIds(): Set<Long>

    fun findByStartTimeBetween(
        from: LocalDateTime,
        to: LocalDateTime,
        pageable: Pageable
    ): Page<Sleep>
}

@Repository
interface SleepScoreRepository :
    JpaRepository<SleepScore, Long>,
    JpaSpecificationExecutor<SleepScore> {

    fun findBySleepLogEntryId(sleepLogEntryId: Long): SleepScore?

    fun findByTimestampBetween(
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
interface DeviceTemperatureRepository : JpaRepository<DeviceTemperature, Long>, JpaSpecificationExecutor<DeviceTemperature>

@Repository
interface DailyRespiratoryRateRepository : JpaRepository<DailyRespiratoryRate, Long>, JpaSpecificationExecutor<DailyRespiratoryRate>

@Repository
interface MinuteSpO2Repository : JpaRepository<MinuteSpO2, Long>, JpaSpecificationExecutor<MinuteSpO2>

@Repository
interface ComputedTemperatureRepository : JpaRepository<ComputedTemperature, Long>, JpaSpecificationExecutor<ComputedTemperature>

@Repository
interface RespiratoryRateSummaryRepository : JpaRepository<RespiratoryRateSummary, Long>, JpaSpecificationExecutor<RespiratoryRateSummary>

@Repository
interface DailySpO2Repository : JpaRepository<DailySpO2, Long>, JpaSpecificationExecutor<DailySpO2>
