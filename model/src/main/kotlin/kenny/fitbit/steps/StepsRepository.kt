package kenny.fitbit.steps

import kenny.fitbit.profile.Profile
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime

data class DailyStepsSum(val date: LocalDate, val totalSteps: Int)

data class HourlyStepsValue(val timeInterval: OffsetDateTime, val sum: Int)

data class WeeklyStepsAverage(val weekNumber: String, val averageSteps: Double)

@Repository
interface StepsRepository :
    JpaRepository<Steps, Long>,
    JpaSpecificationExecutor<Steps> {

    fun findByProfile(profile: Profile, pageable: Pageable): Page<Steps>

    fun findByProfileAndDateTimeBetween(
        profile: Profile,
        from: LocalDateTime,
        to:   LocalDateTime,
        pageable: Pageable
    ): Page<Steps>

    @Query(
        """
        SELECT CAST(s.dateTime AS date)                           AS day,
               SUM(s.value)                                       AS totalSteps
        FROM   Steps s
        WHERE  s.profile = :profile
        AND    s.dateTime BETWEEN :from AND :to
        GROUP  BY CAST(s.dateTime AS date)
        """
    )
    fun sumStepsPerDayBetween(
        profile: Profile,
        from: LocalDateTime,
        to:   LocalDateTime
    ): List<Array<Any>>

    @Query(
        """
    SELECT
        ( CAST(EXTRACT(YEAR FROM s.dateTime)  AS integer) * 100 +
          CAST(EXTRACT(WEEK FROM s.dateTime) AS integer) ) AS week,
        AVG(daily.totalSteps) AS avgSteps
    FROM   Steps s
    JOIN  (
            SELECT CAST(s2.dateTime AS date) AS day,
                   SUM(s2.value)             AS totalSteps
            FROM   Steps s2
            WHERE  s2.profile = :profile
            AND    s2.dateTime BETWEEN :from AND :to
            GROUP  BY CAST(s2.dateTime AS date)
          ) daily
      ON  CAST(s.dateTime AS date) = daily.day
    WHERE  s.profile = :profile
    AND    s.dateTime BETWEEN :from AND :to
    GROUP  BY
           CAST(EXTRACT(YEAR  FROM s.dateTime) AS integer),
           CAST(EXTRACT(WEEK FROM s.dateTime) AS integer)
    """
    )
    fun avgStepsPerWeekBetween(
        profile: Profile,
        from: LocalDateTime,
        to:   LocalDateTime
    ): List<Array<Any>>

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
          COALESCE(SUM(s.value), 0)::int AS val_sum
        FROM buckets b
        LEFT JOIN steps s
          ON s.date_time >= b.bucket_start
         AND s.date_time < b.bucket_start + CAST(:duration AS interval)
         AND s.profile_id = :profileId
        GROUP BY b.bucket_start
        ORDER BY b.bucket_start
    """,
        nativeQuery = true
    )
    fun sumStepsPerInterval(
        @Param("profileId") profileId: String,
        @Param("duration") duration: String = "1 hour",
        @Param("fromDateTime") fromDateTime: LocalDateTime,
        @Param("toDateTime") toDateTime: LocalDateTime
    ): List<Array<Any>>
}
