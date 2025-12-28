package kenny.fitbitkotlin.importer.distance

import com.fasterxml.jackson.databind.JsonNode
import jakarta.persistence.EntityManager
import kenny.fitbitkotlin.distance.Distance
import kenny.fitbitkotlin.distance.DistanceRepository
import kenny.fitbitkotlin.importer.JsonImporter
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import java.time.LocalDateTime

@Component
class DistanceImporterImpl(
    repository: DistanceRepository,
    entityManager: EntityManager,
    transactionManager: PlatformTransactionManager
) : JsonImporter<Distance>(repository, entityManager, transactionManager), DistanceImporter {

    override fun parseToEntity(jsonItem: JsonNode): Distance? {
        val valueStr = jsonItem.get("value")?.asText()
        val dateTimeStr = jsonItem.get("dateTime")?.asText()
        val dateTime: LocalDateTime = LocalDateTime.parse(dateTimeStr ?: "", getDateTimeFormatter())

        return if (valueStr != null) {
            val value = valueStr.toDouble()
            Distance(value, dateTime)
        } else {
            null
        }
    }
}
