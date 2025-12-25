package kenny.fitbitkotlin.heartrate

import kenny.fitbitkotlin.AppleHealthRecord
import kenny.fitbitkotlin.Exporter
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import java.time.LocalDateTime

interface HeartRateExporter : Exporter<HeartRate> {
    override fun healthKitType(): String = "HKQuantityTypeIdentifierHeartRate"
    override fun unit(): String = "count/min"
}

@Component
class HeartRateExporterImpl(
    private val repository: HeartRateRepository
) : HeartRateExporter {

    override fun queryData(from: LocalDateTime, to: LocalDateTime): List<HeartRate> {
        val allData = mutableListOf<HeartRate>()
        var page = 0
        val pageSize = 10000

        do {
            val pageRequest = PageRequest.of(page, pageSize)
            val result = repository.findByTimeBetween(from, to, pageRequest)
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
            unit = unit(),
            value = entity.bpm.toString(),
            creationDate = formattedDate,
            startDate = formattedDate,
            endDate = formattedDate
        )
    }
}
