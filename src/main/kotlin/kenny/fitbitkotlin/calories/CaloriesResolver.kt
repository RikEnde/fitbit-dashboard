package kenny.fitbitkotlin.calories

import kenny.fitbitkotlin.DateRange
import org.springframework.data.domain.PageRequest
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.stereotype.Controller

@Controller
class CaloriesResolver(private val caloriesRepository: CaloriesRepository) {

    @QueryMapping
    fun calories(@Argument limit: Int, @Argument offset: Int, @Argument range: DateRange?): List<Calories> {
        val pageable = PageRequest.of(offset / limit, limit)
        
        return if (range == null) {
            caloriesRepository.findAll(pageable).content
        } else {
            caloriesRepository.findByDateTimeBetween(range.from, range.to, pageable).content
        }
    }
}