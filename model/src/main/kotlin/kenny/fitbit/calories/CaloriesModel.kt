package kenny.fitbit.calories

import jakarta.persistence.*
import kenny.fitbit.profile.Profile
import java.time.LocalDateTime

@Entity
@Table(
    name = "calories",
    indexes = [
        Index(name = "idx_calories_date_time", columnList = "dateTime")
    ]
)
data class Calories(
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