package kenny.fitbit.steps

import kenny.fitbit.AppleHealthRecord
import kenny.fitbit.AuthenticatedProfileService
import kenny.fitbit.Exporter
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import java.time.LocalDateTime

interface StepsExporter : Exporter<Steps> {
    override fun healthKitType(): String = "HKQuantityTypeIdentifierStepCount"
    override fun unit(): String = "count"
}

@Component
class StepsExporterImpl(
    private val repository: StepsRepository,
    private val authService: AuthenticatedProfileService
) : StepsExporter {

    override fun queryData(from: LocalDateTime, to: LocalDateTime): List<Steps> {
        val profile = authService.getProfileOrNull() ?: return emptyList()
        val allData = mutableListOf<Steps>()
        var page = 0
        val pageSize = 10000

        do {
            val pageRequest = PageRequest.of(page, pageSize)
            val result = repository.findByProfileAndDateTimeBetween(profile, from, to, pageRequest)
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
            sourceVersion = sourceVersion,
            unit = unit(),
            value = entity.value.toString(),
            creationDate = formattedDate,
            startDate = formattedDate,
            endDate = formattedDate
        )
    }
}
