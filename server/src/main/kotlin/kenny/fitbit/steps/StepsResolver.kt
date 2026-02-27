package kenny.fitbit.steps

import kenny.fitbit.AuthenticatedProfileService
import kenny.fitbit.DateRange
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.stereotype.Controller

@Controller
class StepsResolver(
    private val stepsRepository: StepsRepository,
    private val authService: AuthenticatedProfileService
) {

    @QueryMapping
    fun steps(@Argument limit: Int, @Argument offset: Int, @Argument range: DateRange?): List<Steps> {
        val profile = authService.getProfileOrNull() ?: return emptyList()
        val effectiveLimit = limit.coerceIn(1, 1000)
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
