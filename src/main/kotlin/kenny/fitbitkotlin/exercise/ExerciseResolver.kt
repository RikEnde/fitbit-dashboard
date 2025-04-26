package kenny.fitbitkotlin.exercise

import kenny.fitbitkotlin.DateRange
import org.springframework.data.domain.PageRequest
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.stereotype.Controller

@Controller
class ExerciseResolver(private val exerciseRepository: ExerciseRepository) {

    @QueryMapping
    fun exercises(@Argument limit: Int, @Argument offset: Int, @Argument range: DateRange?): List<Exercise> {
        val pageable = PageRequest.of(offset / limit, limit)
        
        return if (range == null) {
            exerciseRepository.findAll(pageable).content
        } else {
            exerciseRepository.findByStartTimeBetween(range.from, range.to, pageable).content
        }
    }

    @SchemaMapping(typeName = "Exercise")
    fun heartRateZones(exercise: Exercise): List<HeartRateZone> {
        return exercise.heartRateZones
    }

    @SchemaMapping(typeName = "Exercise")
    fun activityLevels(exercise: Exercise): List<ActivityLevel> {
        return exercise.activityLevels
    }
}