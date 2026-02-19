package kenny.fitbit.heartrate

import kenny.fitbit.AppleHealthRecord
import kenny.fitbit.AuthenticatedProfileService
import kenny.fitbit.Exporter
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import java.time.LocalDateTime

interface HeartRateExporter : Exporter<HeartRate> {
    override fun healthKitType(): String = "HKQuantityTypeIdentifierHeartRate"
    override fun unit(): String = "count/min"
}

@Component
class HeartRateExporterImpl(
    private val repository: HeartRateRepository,
    private val authService: AuthenticatedProfileService
) : HeartRateExporter {

    override fun queryData(from: LocalDateTime, to: LocalDateTime): List<HeartRate> {
        val profile = authService.getProfileOrNull() ?: return emptyList()
        val allData = mutableListOf<HeartRate>()
        var page = 0
        val pageSize = 10000

        do {
            val pageRequest = PageRequest.of(page, pageSize)
            val result = repository.findByProfileAndTimeBetween(profile, from, to, pageRequest)
            allData.addAll(result.content)
            page++
        } while (result.hasNext())

        return allData
    }

    override fun toRecord(entity: HeartRate): AppleHealthRecord {
        val formattedDate = formatDate(entity.time)

        return AppleHealthRecord(
            type = healthKitType(),
            sourceName = sourceName,
            sourceVersion = sourceVersion,
            unit = unit(),
            value = entity.bpm.toString(),
            creationDate = formattedDate,
            startDate = formattedDate,
            endDate = formattedDate
        )
    }
}
