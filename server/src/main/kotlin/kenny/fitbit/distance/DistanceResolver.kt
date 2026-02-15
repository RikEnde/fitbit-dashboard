package kenny.fitbit.distance

import kenny.fitbit.AuthenticatedProfileService
import kenny.fitbit.DateRange
import org.springframework.data.domain.PageRequest
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.stereotype.Controller

@Controller
class DistanceResolver(
    private val distanceRepository: DistanceRepository,
    private val authService: AuthenticatedProfileService
) {

    @QueryMapping
    fun distances(@Argument limit: Int, @Argument offset: Int, @Argument range: DateRange?): List<Distance> {
        val profile = authService.getProfile()
        val pageable = PageRequest.of(offset / limit, limit)

        return if (range == null) {
            distanceRepository.findByProfile(profile, pageable).content
        } else {
            distanceRepository.findByProfileAndDateTimeBetween(profile, range.fromLocal, range.toLocal, pageable).content
        }
    }
}
