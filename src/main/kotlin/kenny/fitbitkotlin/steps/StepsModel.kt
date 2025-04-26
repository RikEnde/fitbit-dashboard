package kenny.fitbitkotlin.steps

import jakarta.persistence.*
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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null
)