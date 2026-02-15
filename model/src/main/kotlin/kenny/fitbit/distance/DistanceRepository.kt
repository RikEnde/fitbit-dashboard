package kenny.fitbit.distance

import kenny.fitbit.profile.Profile
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface DistanceRepository :
    JpaRepository<Distance, Long>,
    JpaSpecificationExecutor<Distance> {

    fun findByProfile(profile: Profile, pageable: Pageable): Page<Distance>

    fun findByProfileAndDateTimeBetween(
        profile: Profile,
        from: LocalDateTime,
        to:   LocalDateTime,
        pageable: Pageable
    ): Page<Distance>
}
