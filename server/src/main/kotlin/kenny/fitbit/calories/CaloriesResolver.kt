package kenny.fitbit.calories

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
class CaloriesResolver(
    private val caloriesRepository: CaloriesRepository,
    private val authService: AuthenticatedProfileService
) {

    @QueryMapping
    fun calories(@Argument limit: Int, @Argument offset: Int, @Argument range: DateRange?): List<Calories> {
        val profile = authService.getProfileOrNull() ?: return emptyList()
        val effectiveLimit = limit.coerceIn(1, 50000)
        val pageable = PageRequest.of(offset / effectiveLimit, effectiveLimit, Sort.by("dateTime").ascending())

        return if (range == null) {
            caloriesRepository.findByProfile(profile, pageable).content
        } else {
            caloriesRepository.findByProfileAndDateTimeBetween(profile, range.fromLocal, range.toLocal, pageable).content
        }
    }

    @QueryMapping
    fun caloriesPerInterval(@Argument range: DateRange, @Argument duration: String?): List<HourlyCaloriesValue> {
        val profile = authService.getProfileOrNull() ?: return emptyList()
        val validDurations = setOf("10 minutes", "15 minutes", "30 minutes", "1 hour")
        val effectiveDuration = if (duration in validDurations) duration!! else "1 hour"
        return caloriesRepository.sumCaloriesPerInterval(
            profileId = profile.id,
            duration = effectiveDuration,
            fromDateTime = range.fromLocal,
            toDateTime = range.toLocal
        ).map {
            HourlyCaloriesValue(
                OffsetDateTime.ofInstant(it[0] as Instant, ZoneOffset.UTC),
                (it[1] as Number).toDouble()
            )
        }
    }

    @QueryMapping
    fun dailyCaloriesSum(@Argument range: DateRange): List<DailyCaloriesSum> {
        val profile = authService.getProfileOrNull() ?: return emptyList()
        val results = caloriesRepository.sumCaloriesPerDayBetween(profile, range.fromLocal, range.toLocal)
        return results.map {
            val date = it[0] as java.sql.Date
            val totalCalories = (it[1] as Number).toDouble()
            DailyCaloriesSum(date.toLocalDate(), totalCalories)
        }
    }
}
