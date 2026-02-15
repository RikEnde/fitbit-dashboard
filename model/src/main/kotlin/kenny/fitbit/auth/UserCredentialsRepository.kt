package kenny.fitbit.auth

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface UserCredentialsRepository : JpaRepository<UserCredentials, Long> {
    fun findByProfile_Username(username: String): UserCredentials?
}
