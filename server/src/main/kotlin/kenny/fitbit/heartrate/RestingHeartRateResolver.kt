package kenny.fitbit.heartrate

import kenny.fitbit.AuthenticatedProfileService
import kenny.fitbit.DateRange
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.stereotype.Controller
import java.time.LocalDate
import java.time.LocalTime

@Controller
class RestingHeartRateResolver(
    private val restingHeartRateRepository: RestingHeartRateRepository,
    private val authService: AuthenticatedProfileService
) {

    @QueryMapping
    fun restingHeartRates(
        @Argument limit: Int,
        @Argument offset: Int,
        @Argument range: DateRange?
    ): List<RestingHeartRate> {
        val profile = authService.getProfile()
        val pageable = PageRequest.of(
            offset / limit, limit,
            Sort.by("dateTime").descending()
        )

        return if (range == null) {
            restingHeartRateRepository.findByProfile(profile, pageable).content
        } else {
            restingHeartRateRepository.findByProfileAndDateTimeBetween(profile, range.fromLocal, range.toLocal, pageable).content
        }
    }

    @QueryMapping
    fun restingHeartRate(@Argument date: LocalDate): RestingHeartRate? {
        val profile = authService.getProfile()
        val startOfDay = date.atStartOfDay()
        val endOfDay = date.atTime(LocalTime.MAX)
        return restingHeartRateRepository.findFirstByProfileAndDateTimeBetweenOrderByDateTimeDesc(profile, startOfDay, endOfDay)
    }
}
