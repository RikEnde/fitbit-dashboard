package kenny.fitbit.distance

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
class DistanceResolver(
    private val distanceRepository: DistanceRepository,
    private val authService: AuthenticatedProfileService
) {

    @QueryMapping
    fun distances(@Argument limit: Int, @Argument offset: Int, @Argument range: DateRange?): List<Distance> {
        val profile = authService.getProfileOrNull() ?: return emptyList()
        val effectiveLimit = limit.coerceIn(1, 50000)
        val pageable = PageRequest.of(offset / effectiveLimit, effectiveLimit, Sort.by("dateTime").ascending())

        return if (range == null) {
            distanceRepository.findByProfile(profile, pageable).content
        } else {
            distanceRepository.findByProfileAndDateTimeBetween(profile, range.fromLocal, range.toLocal, pageable).content
        }
    }

    @QueryMapping
    fun distancePerInterval(@Argument range: DateRange, @Argument duration: String?): List<HourlyDistanceValue> {
        val profile = authService.getProfileOrNull() ?: return emptyList()
        val validDurations = setOf("10 minutes", "15 minutes", "30 minutes", "1 hour")
        val effectiveDuration = if (duration in validDurations) duration!! else "1 hour"
        return distanceRepository.sumDistancePerInterval(
            profileId = profile.id,
            duration = effectiveDuration,
            fromDateTime = range.fromLocal,
            toDateTime = range.toLocal
        ).map {
            HourlyDistanceValue(
                OffsetDateTime.ofInstant(it[0] as Instant, ZoneOffset.UTC),
                (it[1] as Number).toDouble()
            )
        }
    }

    @QueryMapping
    fun dailyDistanceSum(@Argument range: DateRange): List<DailyDistanceSum> {
        val profile = authService.getProfileOrNull() ?: return emptyList()
        val results = distanceRepository.sumDistancePerDayBetween(profile, range.fromLocal, range.toLocal)
        return results.map {
            val date = it[0] as java.sql.Date
            val totalDistance = (it[1] as Number).toDouble()
            DailyDistanceSum(date.toLocalDate(), totalDistance)
        }
    }
}
