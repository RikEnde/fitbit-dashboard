package kenny.fitbit.auth

import jakarta.persistence.*

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
