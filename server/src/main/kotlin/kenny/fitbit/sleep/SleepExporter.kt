package kenny.fitbit.sleep

import kenny.fitbit.AppleHealthRecord
import kenny.fitbit.Exporter
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import java.time.LocalDateTime

/**
 * Apple Health sleep analysis category values:
 * 0 = InBed
 * 1 = Asleep (unspecified)
 * 2 = Awake
 * 3 = Core (light sleep)
 * 4 = Deep
 * 5 = REM
 */
interface SleepExporter : Exporter<SleepLevelData> {
    override fun healthKitType(): String = "HKCategoryTypeIdentifierSleepAnalysis"
    override fun unit(): String = ""  // Category types don't have units

    fun mapFitbitLevelToAppleHealth(level: String): Int
}

@Component
class SleepExporterImpl(
    private val sleepRepository: SleepRepository
) : SleepExporter {

    override fun queryData(from: LocalDateTime, to: LocalDateTime): List<SleepLevelData> {
        val allSleepData = mutableListOf<SleepLevelData>()
        var page = 0
        val pageSize = 100  // Smaller page size since each Sleep has many level entries

        do {
            val pageRequest = PageRequest.of(page, pageSize)
            val sleepPage = sleepRepository.findByStartTimeBetween(from, to, pageRequest)

            for (sleep in sleepPage.content) {
                allSleepData.addAll(sleep.levelData)
            }
            page++
        } while (sleepPage.hasNext())

        return allSleepData
    }

    override fun mapFitbitLevelToAppleHealth(level: String): Int {
        return when (level.lowercase()) {
            "wake", "awake" -> 2      // Awake
            "light" -> 3              // Core (light sleep)
            "deep" -> 4               // Deep
            "rem" -> 5                // REM
            "asleep" -> 1             // Asleep (unspecified)
            else -> 0                 // InBed (default)
        }
    }

    override fun toRecord(entity: SleepLevelData): AppleHealthRecord {
        val startDate = formatDate(entity.dateTime)
        val endDateTime = entity.dateTime.plusSeconds(entity.seconds.toLong())
        val endDate = formatDate(endDateTime)

        return AppleHealthRecord(
            type = healthKitType(),
            sourceName = sourceName,
            sourceVersion = sourceVersion,
            unit = unit(),
            value = mapFitbitLevelToAppleHealth(entity.level).toString(),
            creationDate = startDate,
            startDate = startDate,
            endDate = endDate
        )
    }
}
