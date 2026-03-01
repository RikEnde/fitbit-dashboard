package kenny.fitbit.exercise

import kenny.fitbit.AuthenticatedProfileService
import kenny.fitbit.DateRange
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.stereotype.Controller

@Controller
class ExerciseResolver(
    private val exerciseRepository: ExerciseRepository,
    private val authService: AuthenticatedProfileService
) {

    @QueryMapping
    fun exercises(@Argument limit: Int, @Argument offset: Int, @Argument range: DateRange?): List<Exercise> {
        val profile = authService.getProfileOrNull() ?: return emptyList()
        val effectiveLimit = limit.coerceIn(1, 50000)
        val pageable = PageRequest.of(offset / effectiveLimit, effectiveLimit, Sort.by("startTime").ascending())

        return if (range == null) {
            exerciseRepository.findByProfile(profile, pageable).content
        } else {
            exerciseRepository.findByProfileAndStartTimeBetween(profile, range.fromLocal, range.toLocal, pageable).content
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
