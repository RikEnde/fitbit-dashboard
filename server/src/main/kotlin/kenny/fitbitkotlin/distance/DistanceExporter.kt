package kenny.fitbitkotlin.distance

import kenny.fitbitkotlin.AppleHealthRecord
import kenny.fitbitkotlin.Exporter
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import java.time.LocalDateTime

interface DistanceExporter : Exporter<Distance> {
    override fun healthKitType(): String = "HKQuantityTypeIdentifierDistanceWalkingRunning"
    override fun unit(): String = "km"
}

@Component
class DistanceExporterImpl(
    private val repository: DistanceRepository
) : DistanceExporter {

    override fun queryData(from: LocalDateTime, to: LocalDateTime): List<Distance> {
        val allData = mutableListOf<Distance>()
        var page = 0
        val pageSize = 10000

        do {
            val pageRequest = PageRequest.of(page, pageSize)
            val result = repository.findByDateTimeBetween(from, to, pageRequest)
            allData.addAll(result.content)
            page++
        } while (result.hasNext())

        return allData
    }

    override fun toRecord(entity: Distance): AppleHealthRecord {
        val formattedDate = formatDate(entity.dateTime)

        return AppleHealthRecord(
            type = healthKitType(),
            sourceName = sourceName,
            sourceVersion = sourceVersion,
            unit = unit(),
            value = entity.value.toString(),
            creationDate = formattedDate,
            startDate = formattedDate,
            endDate = formattedDate
        )
    }
}
