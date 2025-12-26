package kenny.fitbitkotlin.steps

import kenny.fitbitkotlin.AppleHealthRecord
import kenny.fitbitkotlin.Exporter
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import java.time.LocalDateTime

interface StepsExporter : Exporter<Steps> {
    override fun healthKitType(): String = "HKQuantityTypeIdentifierStepCount"
    override fun unit(): String = "count"
}

@Component
class StepsExporterImpl(
    private val repository: StepsRepository
) : StepsExporter {

    override fun queryData(from: LocalDateTime, to: LocalDateTime): List<Steps> {
        val allData = mutableListOf<Steps>()
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

    override fun toRecord(entity: Steps): AppleHealthRecord {
        val formattedDate = formatDate(entity.dateTime)

        return AppleHealthRecord(
            type = healthKitType(),
            sourceName = sourceName,
            unit = unit(),
            value = entity.value.toString(),
            creationDate = formattedDate,
            startDate = formattedDate,
            endDate = formattedDate
        )
    }
}
