package kenny.fitbitkotlin.distance

import jakarta.persistence.*
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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null
)