package kenny.fitbit.calories

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

    fun findByDateTimeBetween(
        from: LocalDateTime,
        to:   LocalDateTime,
        pageable: Pageable
    ): Page<Calories>
}
