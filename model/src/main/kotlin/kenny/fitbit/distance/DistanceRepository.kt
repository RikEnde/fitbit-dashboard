package kenny.fitbit.distance

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

data class DailyDistanceSum(val date: LocalDate, val totalDistance: Double)

data class HourlyDistanceValue(val timeInterval: OffsetDateTime, val sum: Double)

@Repository
interface DistanceRepository :
    JpaRepository<Distance, Long>,
    JpaSpecificationExecutor<Distance> {

    fun findByProfile(profile: Profile, pageable: Pageable): Page<Distance>

    fun findByProfileAndDateTimeBetween(
        profile: Profile,
        from: LocalDateTime,
        to:   LocalDateTime,
        pageable: Pageable
    ): Page<Distance>

    @Query(
        """
        SELECT CAST(d.dateTime AS date)  AS day,
               SUM(d.value)              AS totalDistance
        FROM   Distance d
        WHERE  d.profile = :profile
        AND    d.dateTime BETWEEN :from AND :to
        GROUP  BY CAST(d.dateTime AS date)
        """
    )
    fun sumDistancePerDayBetween(
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
          COALESCE(SUM(d.value), 0)::float AS val_sum
        FROM buckets b
        LEFT JOIN distance d
          ON d.date_time >= b.bucket_start
         AND d.date_time < b.bucket_start + CAST(:duration AS interval)
         AND d.profile_id = :profileId
        GROUP BY b.bucket_start
        ORDER BY b.bucket_start
    """,
        nativeQuery = true
    )
    fun sumDistancePerInterval(
        @Param("profileId") profileId: String,
        @Param("duration") duration: String = "1 hour",
        @Param("fromDateTime") fromDateTime: LocalDateTime,
        @Param("toDateTime") toDateTime: LocalDateTime
    ): List<Array<Any>>
}
