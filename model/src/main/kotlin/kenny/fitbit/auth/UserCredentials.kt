package kenny.fitbit.auth

import jakarta.persistence.*

// Intentionally has no Profile FK: user accounts are created at registration, before any data
// is imported. A Profile only exists after a Fitbit export is imported. The username is used to
// link credentials to a profile at query time via AuthenticatedProfileService.
@Entity
@Table(name = "user_credentials")
data class UserCredentials(
    @Column(nullable = false, unique = true)
    val username: String,

    @Column(nullable = false)
    val hash: String,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null
)
