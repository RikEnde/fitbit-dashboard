package kenny.fitbit.calories

import com.fasterxml.jackson.databind.JsonNode
import jakarta.persistence.EntityManager
import kenny.fitbit.JsonImporter
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import java.time.LocalDate
import java.time.LocalDateTime

@Component
class CaloriesImporterImpl(
    repository: CaloriesRepository,
    entityManager: EntityManager,
    transactionManager: PlatformTransactionManager
) : JsonImporter<Calories>(repository, entityManager, transactionManager), CaloriesImporter {

    override fun entityDate(entity: Calories): LocalDate = entity.dateTime.toLocalDate()

    override fun parseToEntity(jsonItem: JsonNode): Calories? {
        val valueStr = jsonItem.get("value")?.asText()
        val dateTimeStr = jsonItem.get("dateTime")?.asText()
        val dateTime: LocalDateTime = LocalDateTime.parse(dateTimeStr ?: "", getDateTimeFormatter())

        return if (valueStr != null) {
            val value = valueStr.toDouble()
            Calories(value, dateTime, profile!!)
        } else {
            null
        }
    }
}
