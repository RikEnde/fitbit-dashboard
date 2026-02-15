package kenny.fitbit.importlog

import jakarta.persistence.*
import kenny.fitbit.profile.Profile
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(
    name = "import_log",
    indexes = [
        Index(name = "idx_import_log_profile_id", columnList = "profile_id"),
        Index(name = "idx_import_log_latest_data_date", columnList = "latestDataDate")
    ]
)
data class ImportLog(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    val profile: Profile,

    @Column(nullable = false)
    val statType: String,

    @Column(nullable = false)
    val latestDataDate: LocalDate,

    @Column(nullable = false)
    val importedAt: LocalDateTime,

    @Column(nullable = false)
    val fileCount: Int,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null
)
