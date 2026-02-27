package kenny.fitbit.calories

import kenny.fitbit.AuthenticatedProfileService
import kenny.fitbit.DateRange
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.stereotype.Controller

@Controller
class CaloriesResolver(
    private val caloriesRepository: CaloriesRepository,
    private val authService: AuthenticatedProfileService
) {

    @QueryMapping
    fun calories(@Argument limit: Int, @Argument offset: Int, @Argument range: DateRange?): List<Calories> {
        val profile = authService.getProfileOrNull() ?: return emptyList()
        val effectiveLimit = limit.coerceIn(1, 1000)
        val pageable = PageRequest.of(offset / effectiveLimit, effectiveLimit, Sort.by("dateTime").ascending())

        return if (range == null) {
            caloriesRepository.findByProfile(profile, pageable).content
        } else {
            caloriesRepository.findByProfileAndDateTimeBetween(profile, range.fromLocal, range.toLocal, pageable).content
        }
    }
}
