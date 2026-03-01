package kenny.fitbit.heartrate

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
class HeartRateResolver(
    private val heartRateRepository: HeartRateRepository,
    private val authService: AuthenticatedProfileService
) {

    @QueryMapping
    fun heartRates(@Argument limit: Int, @Argument offset: Int, @Argument range: DateRange?): List<HeartRate> {
        val profile = authService.getProfileOrNull() ?: return emptyList()
        val effectiveLimit = limit.coerceIn(1, 50000)
        val pageable = PageRequest.of(offset / effectiveLimit, effectiveLimit,
            Sort.by("dateTime").ascending())

        return if (range == null) {
            heartRateRepository.findByProfile(profile, pageable).content
        } else {
            heartRateRepository.findByProfileAndDateTimeBetween(profile, range.fromLocal, range.toLocal, pageable).content
        }
    }

    @QueryMapping
    fun heartRatesPerInterval(@Argument range: DateRange, @Argument duration: String?): List<SumsOfHeartRates> {
        val profile = authService.getProfileOrNull() ?: return emptyList()
        val validDurations = setOf("10 minutes", "15 minutes", "30 minutes", "1 hour")
        val effectiveDuration = if (duration in validDurations) duration!! else "10 minutes"
        return heartRateRepository.sumByTimeBetween(
            profileId = profile.id,
            duration = effectiveDuration,
            fromDateTime = range.fromLocal,
            toDateTime = range.toLocal
        ).map {
            SumsOfHeartRates(
                OffsetDateTime.ofInstant(it[0] as Instant, ZoneOffset.UTC),
                it[1] as Int,
                it[2] as Int
            )
        }
    }
}
