package kenny.fitbit.sleep

import kenny.fitbit.AuthenticatedProfileService
import kenny.fitbit.DateRange
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.stereotype.Controller

@Controller
class SleepResolver(
    private val sleepRepository: SleepRepository,
    private val authService: AuthenticatedProfileService
) {

    @QueryMapping
    fun sleeps(@Argument limit: Int, @Argument offset: Int, @Argument range: DateRange?): List<Sleep> {
        val profile = authService.getProfileOrNull() ?: return emptyList()
        val pageable = PageRequest.of(offset / limit, limit, Sort.by("startTime").ascending())

        return if (range == null) {
            sleepRepository.findByProfile(profile, pageable).content
        } else {
            sleepRepository.findByProfileAndStartTimeBetween(profile, range.fromLocal, range.toLocal, pageable).content
        }
    }

    @SchemaMapping(typeName = "Sleep")
    fun levelSummaries(sleep: Sleep): List<SleepLevelSummary> {
        return sleep.levelSummaries
    }

    @SchemaMapping(typeName = "Sleep")
    fun levelData(sleep: Sleep): List<SleepLevelData> {
        return sleep.levelData
    }

    @SchemaMapping(typeName = "Sleep")
    fun levelShortData(sleep: Sleep): List<SleepLevelShortData> {
        return sleep.levelShortData
    }
}

@Controller
class SleepScoreResolver(
    private val sleepScoreRepository: SleepScoreRepository,
    private val authService: AuthenticatedProfileService
) {

    @QueryMapping
    fun sleepScores(@Argument limit: Int, @Argument offset: Int, @Argument range: DateRange?): List<SleepScore> {
        val profile = authService.getProfileOrNull() ?: return emptyList()
        val pageable = PageRequest.of(offset / limit, limit, Sort.by("timestamp").ascending())

        return if (range == null) {
            sleepScoreRepository.findByProfile(profile, pageable).content
        } else {
            sleepScoreRepository.findByProfileAndTimestampBetween(profile, range.fromLocal, range.toLocal, pageable).content
        }
    }
}
