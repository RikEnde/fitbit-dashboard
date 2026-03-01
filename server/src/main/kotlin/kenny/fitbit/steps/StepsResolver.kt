package kenny.fitbit.steps

import kenny.fitbit.AuthenticatedProfileService
import kenny.fitbit.DateRange
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.stereotype.Controller
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Controller
class StepsResolver(
    private val stepsRepository: StepsRepository,
    private val authService: AuthenticatedProfileService
) {

    @QueryMapping
    fun steps(@Argument limit: Int, @Argument offset: Int, @Argument range: DateRange?): List<Steps> {
        val profile = authService.getProfileOrNull() ?: return emptyList()
        val effectiveLimit = limit.coerceIn(1, 50000)
        val pageable = PageRequest.of(offset / effectiveLimit, effectiveLimit,
            Sort.by("dateTime").ascending())

        return if (range == null) {
            stepsRepository.findByProfile(profile, pageable).content
        } else {
            stepsRepository.findByProfileAndDateTimeBetween(profile, range.fromLocal, range.toLocal, pageable).content
        }
    }

    @QueryMapping
    fun dailyStepsSum(@Argument range: DateRange): List<DailyStepsSum> {
        val profile = authService.getProfileOrNull() ?: return emptyList()
        val results = stepsRepository.sumStepsPerDayBetween(profile, range.fromLocal, range.toLocal)
        return results.map {
            val date = it[0] as java.sql.Date
            val totalSteps = (it[1] as Number).toInt()
            DailyStepsSum(date.toLocalDate(), totalSteps)
        }
    }

    @QueryMapping
    fun stepsPerInterval(@Argument range: DateRange, @Argument duration: String?): List<HourlyStepsValue> {
        val profile = authService.getProfileOrNull() ?: return emptyList()
        val validDurations = setOf("10 minutes", "15 minutes", "30 minutes", "1 hour")
        val effectiveDuration = if (duration in validDurations) duration!! else "1 hour"
        return stepsRepository.sumStepsPerInterval(
            profileId = profile.id,
            duration = effectiveDuration,
            fromDateTime = range.fromLocal,
            toDateTime = range.toLocal
        ).map {
            HourlyStepsValue(
                OffsetDateTime.ofInstant(it[0] as Instant, ZoneOffset.UTC),
                it[1] as Int
            )
        }
    }

    @QueryMapping
    fun weeklyStepsAverage(@Argument range: DateRange): List<WeeklyStepsAverage> {
        val profile = authService.getProfileOrNull() ?: return emptyList()
        val results = stepsRepository.avgStepsPerWeekBetween(profile, range.fromLocal, range.toLocal)
        return results.map {
            val weekNumber = (it[0] as Number).toString()
            val averageSteps = (it[1] as Number).toDouble()
            WeeklyStepsAverage(weekNumber, averageSteps)
        }
    }
}
