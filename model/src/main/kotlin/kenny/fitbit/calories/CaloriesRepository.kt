package kenny.fitbit.calories

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

data class DailyCaloriesSum(val date: LocalDate, val totalCalories: Double)

data class HourlyCaloriesValue(val timeInterval: OffsetDateTime, val sum: Double)

@Repository
interface CaloriesRepository :
    JpaRepository<Calories, Long>,
    JpaSpecificationExecutor<Calories> {

    fun findByProfile(profile: Profile, pageable: Pageable): Page<Calories>

    fun findByProfileAndDateTimeBetween(
        profile: Profile,
        from: LocalDateTime,
        to:   LocalDateTime,
        pageable: Pageable
    ): Page<Calories>

    @Query(
        """
        SELECT CAST(c.dateTime AS date)  AS day,
               SUM(c.value)              AS totalCalories
        FROM   Calories c
        WHERE  c.profile = :profile
        AND    c.dateTime BETWEEN :from AND :to
        GROUP  BY CAST(c.dateTime AS date)
        """
    )
    fun sumCaloriesPerDayBetween(
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
          COALESCE(SUM(c.value), 0)::float AS val_sum
        FROM buckets b
        LEFT JOIN calories c
          ON c.date_time >= b.bucket_start
         AND c.date_time < b.bucket_start + CAST(:duration AS interval)
         AND c.profile_id = :profileId
        GROUP BY b.bucket_start
        ORDER BY b.bucket_start
    """,
        nativeQuery = true
    )
    fun sumCaloriesPerInterval(
        @Param("profileId") profileId: String,
        @Param("duration") duration: String = "1 hour",
        @Param("fromDateTime") fromDateTime: LocalDateTime,
        @Param("toDateTime") toDateTime: LocalDateTime
    ): List<Array<Any>>
}
