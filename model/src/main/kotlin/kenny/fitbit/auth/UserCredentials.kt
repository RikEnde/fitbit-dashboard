package kenny.fitbit.auth

import jakarta.persistence.*
import kenny.fitbit.profile.Profile

@Entity
@Table(name = "user_credentials")
data class UserCredentials(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false, unique = true)
    val profile: Profile,

    @Column(nullable = false)
    val hash: String,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null
)
