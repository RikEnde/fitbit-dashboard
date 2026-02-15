package kenny.fitbit.heartrate

import kenny.fitbit.profile.Profile
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.time.OffsetDateTime

data class SumsOfHeartRates(
    val timeInterval: OffsetDateTime,
    val bpmSum: Int);

@Repository
interface HeartRateRepository : JpaRepository<HeartRate, Long>, JpaSpecificationExecutor<HeartRate> {
    fun findByProfileAndTimeBetween(profile: Profile, from: LocalDateTime, to: LocalDateTime, pageable: Pageable): Page<HeartRate>
    fun findByProfile(profile: Profile, pageable: Pageable): Page<HeartRate>

    @Query(
        value = """
        WITH buckets AS (
          SELECT generate_series(
                   :fromDateTime,
                   :toDateTime,
                   CAST(:duration AS interval)
                 ) AS bucket_start
        )
        SELECT
          b.bucket_start AS time_interval,
          COALESCE(SUM(hr.bpm), 0)::int AS bpm_sum
        FROM buckets b
        LEFT JOIN heart_rates hr
          ON hr.time >= b.bucket_start
         AND hr.time < b.bucket_start + CAST(:duration AS interval)
         AND hr.profile_id = :profileId
        GROUP BY b.bucket_start
        ORDER BY b.bucket_start
    """,
        nativeQuery = true
    )
    fun sumByTimeBetween(
        @Param("profileId") profileId: String,
        @Param("duration") duration: String = "10 minutes",
        @Param("fromDateTime") fromDateTime: LocalDateTime,
        @Param("toDateTime") toDateTime: LocalDateTime
    ): List<Array<Any>>
}

@Repository
interface RestingHeartRateRepository : JpaRepository<RestingHeartRate, Long>, JpaSpecificationExecutor<RestingHeartRate> {
    fun findByProfileAndDateTimeBetween(profile: Profile, from: LocalDateTime, to: LocalDateTime, pageable: Pageable): Page<RestingHeartRate>
    fun findFirstByProfileAndDateTimeBetweenOrderByDateTimeDesc(profile: Profile, from: LocalDateTime, to: LocalDateTime): RestingHeartRate?
    fun findByProfile(profile: Profile, pageable: Pageable): Page<RestingHeartRate>
}

@Repository
interface DailyHeartRateVariabilityRepository : JpaRepository<DailyHeartRateVariability, Long>, JpaSpecificationExecutor<DailyHeartRateVariability>

@Repository
interface HeartRateVariabilityDetailsRepository : JpaRepository<HeartRateVariabilityDetails, Long>, JpaSpecificationExecutor<HeartRateVariabilityDetails>
