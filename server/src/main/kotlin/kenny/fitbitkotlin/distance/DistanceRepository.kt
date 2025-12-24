package kenny.fitbitkotlin.distance

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

    fun findByDateTimeBetween(
        from: LocalDateTime,
        to:   LocalDateTime,
        pageable: Pageable
    ): Page<Distance>
}
