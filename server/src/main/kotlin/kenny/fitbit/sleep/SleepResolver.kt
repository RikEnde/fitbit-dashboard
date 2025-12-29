package kenny.fitbit.sleep

import kenny.fitbit.DateRange
import org.springframework.data.domain.PageRequest
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.stereotype.Controller

@Controller
class SleepResolver(private val sleepRepository: SleepRepository) {

    @QueryMapping
    fun sleeps(@Argument limit: Int, @Argument offset: Int, @Argument range: DateRange?): List<Sleep> {
        val pageable = PageRequest.of(offset / limit, limit)
        
        return if (range == null) {
            sleepRepository.findAll(pageable).content
        } else {
            sleepRepository.findByStartTimeBetween(range.from, range.to, pageable).content
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
class SleepScoreResolver(private val sleepScoreRepository: SleepScoreRepository) {

    @QueryMapping
    fun sleepScores(@Argument limit: Int, @Argument offset: Int, @Argument range: DateRange?): List<SleepScore> {
        val pageable = PageRequest.of(offset / limit, limit)
        
        return if (range == null) {
            sleepScoreRepository.findAll(pageable).content
        } else {
            sleepScoreRepository.findByTimestampBetween(range.from, range.to, pageable).content
        }
    }
}