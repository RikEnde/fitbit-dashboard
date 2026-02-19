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
        val pageable = PageRequest.of(offset / limit, limit,
            Sort.by("time").ascending())

        return if (range == null) {
            heartRateRepository.findByProfile(profile, pageable).content
        } else {
            heartRateRepository.findByProfileAndTimeBetween(profile, range.fromLocal, range.toLocal, pageable).content
        }
    }

    @QueryMapping
    fun heartRatesPerInterval(@Argument range: DateRange): List<SumsOfHeartRates> {
        val profile = authService.getProfileOrNull() ?: return emptyList()
        return heartRateRepository.sumByTimeBetween(
            profileId = profile.id,
            duration = "10 minutes",
            fromDateTime = range.fromLocal,
            toDateTime = range.toLocal
        ).map {
            SumsOfHeartRates(
                OffsetDateTime.ofInstant(it[0] as Instant, ZoneOffset.UTC),
                it[1] as Int
            )
        }
    }
}
