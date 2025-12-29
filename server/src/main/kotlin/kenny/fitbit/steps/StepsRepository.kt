package kenny.fitbit.steps

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.LocalDateTime

data class DailyStepsSum(val date: LocalDate, val totalSteps: Int)

data class WeeklyStepsAverage(val weekNumber: String, val averageSteps: Double)

@Repository
interface StepsRepository :
    JpaRepository<Steps, Long>,
    JpaSpecificationExecutor<Steps> {

    fun findByDateTimeBetween(
        from: LocalDateTime,
        to:   LocalDateTime,
        pageable: Pageable
    ): Page<Steps>

    /** day→sum */
    @Query(
        """
        SELECT CAST(s.dateTime AS date)                           AS day,
               SUM(s.value)                                       AS totalSteps
        FROM   Steps s
        WHERE  s.dateTime BETWEEN :from AND :to
        GROUP  BY CAST(s.dateTime AS date)
        """
    )
    fun sumStepsPerDayBetween(
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
            WHERE  s2.dateTime BETWEEN :from AND :to
            GROUP  BY CAST(s2.dateTime AS date)
          ) daily
      ON  CAST(s.dateTime AS date) = daily.day
    WHERE  s.dateTime BETWEEN :from AND :to
    GROUP  BY
           CAST(EXTRACT(YEAR  FROM s.dateTime) AS integer),
           CAST(EXTRACT(WEEK FROM s.dateTime) AS integer)
    """
    )
    fun avgStepsPerWeekBetween(
        from: LocalDateTime,
        to:   LocalDateTime
    ): List<Array<Any>>
}
