package kenny.fitbitkotlin.distance

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import java.io.File
import java.time.LocalDateTime

@Component
class DistanceImporterImpl(
    val repository: DistanceRepository,
    val batchService: kenny.fitbitkotlin.TransactionalBatchService
): DistanceImporter {

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

    override fun parseAndSaveEntity(jsonItem: JsonNode) {
        // Not used - parseToEntity handles it
    }

    override suspend fun importFile(
        index: Int,
        size: Int,
        file: File,
        objectMapper: ObjectMapper
    ) {
        importFileWithBatching(index, size, file, objectMapper, batchService) { batch ->
            repository.saveAll(batch)
        }
    }
}