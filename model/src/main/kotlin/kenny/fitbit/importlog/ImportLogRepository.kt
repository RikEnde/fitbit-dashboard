package kenny.fitbit.importlog

import kenny.fitbit.profile.Profile
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ImportLogRepository : JpaRepository<ImportLog, Long> {
    fun findTopByProfileOrderByLatestDataDateDesc(profile: Profile): ImportLog?
}
