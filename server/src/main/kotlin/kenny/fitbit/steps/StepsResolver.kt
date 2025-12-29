package kenny.fitbit.steps

import kenny.fitbit.DateRange
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.stereotype.Controller

@Controller
class StepsResolver(private val stepsRepository: StepsRepository) {

    @QueryMapping
    fun steps(@Argument limit: Int, @Argument offset: Int, @Argument range: DateRange?): List<Steps> {

        val pageable = PageRequest.of(offset / limit, limit,
            Sort.by("dateTime").ascending())

        return if (range == null) {
            stepsRepository.findAll(pageable).content
        } else {
            stepsRepository.findByDateTimeBetween(range.fromLocal, range.toLocal, pageable).content
        }
    }

    @QueryMapping
    fun dailyStepsSum(@Argument range: DateRange): List<DailyStepsSum> {
        val results = stepsRepository.sumStepsPerDayBetween(range.fromLocal, range.toLocal)
        return results.map {
            val date = it[0] as java.sql.Date
            val totalSteps = (it[1] as Number).toInt()
            DailyStepsSum(date.toLocalDate(), totalSteps)
        }
    }

    @QueryMapping
    fun weeklyStepsAverage(@Argument range: DateRange): List<WeeklyStepsAverage> {
        val results = stepsRepository.avgStepsPerWeekBetween(range.fromLocal, range.toLocal)
        return results.map {
            val weekNumber = (it[0] as Number).toString()
            val averageSteps = (it[1] as Number).toDouble()
            WeeklyStepsAverage(weekNumber, averageSteps)
        }
    }
}
