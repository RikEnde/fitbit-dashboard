package kenny.fitbit.calories

import kenny.fitbit.AppleHealthRecord
import kenny.fitbit.Exporter
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import java.time.LocalDateTime

interface CaloriesExporter : Exporter<Calories> {
    override fun healthKitType(): String = "HKQuantityTypeIdentifierActiveEnergyBurned"
    override fun unit(): String = "kcal"
}

@Component
class CaloriesExporterImpl(
    private val repository: CaloriesRepository
) : CaloriesExporter {

    override fun queryData(from: LocalDateTime, to: LocalDateTime): List<Calories> {
        val allData = mutableListOf<Calories>()
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

    override fun toRecord(entity: Calories): AppleHealthRecord {
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
