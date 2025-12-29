package kenny.fitbit.importer.calories

import com.fasterxml.jackson.databind.JsonNode
import jakarta.persistence.EntityManager
import kenny.fitbit.calories.Calories
import kenny.fitbit.calories.CaloriesRepository
import kenny.fitbit.importer.JsonImporter
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import java.time.LocalDateTime

@Component
class CaloriesImporterImpl(
    repository: CaloriesRepository,
    entityManager: EntityManager,
    transactionManager: PlatformTransactionManager
) : JsonImporter<Calories>(repository, entityManager, transactionManager), CaloriesImporter {

    override fun parseToEntity(jsonItem: JsonNode): Calories? {
        val valueStr = jsonItem.get("value")?.asText()
        val dateTimeStr = jsonItem.get("dateTime")?.asText()
        val dateTime: LocalDateTime = LocalDateTime.parse(dateTimeStr ?: "", getDateTimeFormatter())

        return if (valueStr != null) {
            val value = valueStr.toDouble()
            Calories(value, dateTime)
        } else {
            null
        }
    }
}
