package kenny.fitbitkotlin.distance

import kenny.fitbitkotlin.DateRange
import org.springframework.data.domain.PageRequest
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.stereotype.Controller

@Controller
class DistanceResolver(private val distanceRepository: DistanceRepository) {

    @QueryMapping
    fun distances(@Argument limit: Int, @Argument offset: Int, @Argument range: DateRange?): List<Distance> {
        val pageable = PageRequest.of(offset / limit, limit)
        
        return if (range == null) {
            distanceRepository.findAll(pageable).content
        } else {
            distanceRepository.findByDateTimeBetween(range.from, range.to, pageable).content
        }
    }
}