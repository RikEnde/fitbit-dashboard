package kenny.fitbitkotlin.steps

import com.fasterxml.jackson.databind.JsonNode
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class StepsImporterImpl(val repository: StepsRepository): StepsImporter {

    override fun parseAndSaveEntity(jsonItem: JsonNode) {
        val valueStr = jsonItem.get("value")?.asText()
        val dateTimeStr = jsonItem.get("dateTime")?.asText()
        val dateTime: LocalDateTime = LocalDateTime.parse(dateTimeStr ?: "", getDateTimeFormatter())

        if (valueStr != null) {
            val value = valueStr.toInt()
            val steps = Steps(value, dateTime)
            repository.save(steps)
        }
    }
}