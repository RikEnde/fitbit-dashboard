package kenny.fitbitkotlin.calories

import com.fasterxml.jackson.databind.JsonNode
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class CaloriesImporterImpl(val repository: CaloriesRepository): CaloriesImporter {

    override fun parseAndSaveEntity(jsonItem: JsonNode) {
        val valueStr = jsonItem.get("value")?.asText()
        val dateTimeStr = jsonItem.get("dateTime")?.asText()
        val dateTime: LocalDateTime = LocalDateTime.parse(dateTimeStr ?: "", getDateTimeFormatter())

        if (valueStr != null) {
            val value = valueStr.toDouble()
            val calories = Calories(value, dateTime)
            repository.save(calories)
        }
    }
}