package kenny.fitbit.importer.steps

import com.fasterxml.jackson.databind.JsonNode
import jakarta.persistence.EntityManager
import kenny.fitbit.importer.JsonImporter
import kenny.fitbit.steps.Steps
import kenny.fitbit.steps.StepsRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import java.time.LocalDateTime

@Component
class StepsImporterImpl(
    repository: StepsRepository,
    entityManager: EntityManager,
    transactionManager: PlatformTransactionManager
) : JsonImporter<Steps>(repository, entityManager, transactionManager), StepsImporter {

    override fun parseToEntity(jsonItem: JsonNode): Steps? {
        val valueStr = jsonItem.get("value")?.asText()
        val dateTimeStr = jsonItem.get("dateTime")?.asText()
        val dateTime: LocalDateTime = LocalDateTime.parse(dateTimeStr ?: "", getDateTimeFormatter())

        return if (valueStr != null) {
            val value = valueStr.toInt()
            Steps(value, dateTime)
        } else {
            null
        }
    }
}
