package kenny.fitbit.steps

import jakarta.persistence.*
import kenny.fitbit.profile.Profile
import java.time.LocalDateTime

@Entity
@Table(
    name = "steps",
    indexes = [
        Index(name = "idx_steps_date_time", columnList = "dateTime")
    ]
)
data class Steps(
    @Column(nullable = false)
    val value: Int,

    @Column(nullable = false)
    val dateTime: LocalDateTime,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    val profile: Profile,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null
)