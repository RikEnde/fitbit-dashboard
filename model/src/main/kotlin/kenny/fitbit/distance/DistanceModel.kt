package kenny.fitbit.distance

import jakarta.persistence.*
import kenny.fitbit.profile.Profile
import java.time.LocalDateTime

@Entity
@Table(
    name = "distance",
    indexes = [
        Index(name = "idx_distance_date_time", columnList = "dateTime")
    ]
)
data class Distance(
    @Column(nullable = false)
    val value: Double,

    @Column(nullable = false)
    val dateTime: LocalDateTime,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    val profile: Profile,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null
)