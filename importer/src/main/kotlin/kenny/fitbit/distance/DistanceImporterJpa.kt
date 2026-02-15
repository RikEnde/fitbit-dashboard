package kenny.fitbit.distance

import com.fasterxml.jackson.databind.JsonNode
import jakarta.persistence.EntityManager
import kenny.fitbit.JsonImporter
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import java.time.LocalDate
import java.time.LocalDateTime

@Component
class DistanceImporterImpl(
    repository: DistanceRepository,
    entityManager: EntityManager,
    transactionManager: PlatformTransactionManager
) : JsonImporter<Distance>(repository, entityManager, transactionManager), DistanceImporter {

    override fun entityDate(entity: Distance): LocalDate = entity.dateTime.toLocalDate()

    override fun parseToEntity(jsonItem: JsonNode): Distance? {
        val valueStr = jsonItem.get("value")?.asText()
        val dateTimeStr = jsonItem.get("dateTime")?.asText()
        val dateTime: LocalDateTime = LocalDateTime.parse(dateTimeStr ?: "", getDateTimeFormatter())

        return if (valueStr != null) {
            val value = valueStr.toDouble()
            Distance(value, dateTime, profile!!)
        } else {
            null
        }
    }
}
