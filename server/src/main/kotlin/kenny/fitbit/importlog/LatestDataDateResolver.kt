package kenny.fitbit.importlog

import kenny.fitbit.AuthenticatedProfileService
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.stereotype.Controller
import java.time.LocalDate

@Controller
class LatestDataDateResolver(
    private val importLogRepository: ImportLogRepository,
    private val authService: AuthenticatedProfileService
) {

    @QueryMapping
    fun latestDataDate(): LocalDate? {
        val profile = authService.getProfile()
        return importLogRepository.findTopByProfileOrderByLatestDataDateDesc(profile)?.latestDataDate
    }
}
