package kenny.fitbit.calories

import kenny.fitbit.profile.Profile
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface CaloriesRepository :
    JpaRepository<Calories, Long>,
    JpaSpecificationExecutor<Calories> {

    fun findByProfile(profile: Profile, pageable: Pageable): Page<Calories>

    fun findByProfileAndDateTimeBetween(
        profile: Profile,
        from: LocalDateTime,
        to:   LocalDateTime,
        pageable: Pageable
    ): Page<Calories>
}
