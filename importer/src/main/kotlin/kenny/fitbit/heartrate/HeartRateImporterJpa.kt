package kenny.fitbit.heartrate

import com.fasterxml.jackson.databind.JsonNode
import jakarta.persistence.EntityManager
import kenny.fitbit.CsvImporter
import kenny.fitbit.JsonImporter
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Component
class HeartRateImporterImpl(
    repository: HeartRateRepository,
    entityManager: EntityManager,
    transactionManager: PlatformTransactionManager
) : JsonImporter<HeartRate>(repository, entityManager, transactionManager), HeartRateImporter {

    override fun entityDate(entity: HeartRate): LocalDate = entity.time.toLocalDate()

    override fun parseToEntity(jsonItem: JsonNode): HeartRate? {
        val bpm = jsonItem.get("value")?.get("bpm")?.asInt()
        val confidence = jsonItem.get("value")?.get("confidence")?.asInt()
        val dateTimeStr = jsonItem.get("dateTime")?.asText()
        val dateTime: LocalDateTime = LocalDateTime.parse(dateTimeStr ?: "", getDateTimeFormatter())

        return if (bpm != null && confidence != null) {
            HeartRate(bpm, confidence, dateTime, profile!!)
        } else {
            null
        }
    }
}

@Component
class RestingHeartRateImporterImpl(
    repository: RestingHeartRateRepository,
    entityManager: EntityManager,
    transactionManager: PlatformTransactionManager
) : JsonImporter<RestingHeartRate>(repository, entityManager, transactionManager), RestingHeartRateImporter {

    override fun entityDate(entity: RestingHeartRate): LocalDate = entity.dateTime.toLocalDate()

    override fun parseToEntity(jsonItem: JsonNode): RestingHeartRate? {
        val valueNode = jsonItem.get("value")
        val value = valueNode?.get("value")?.asDouble()
        val error = valueNode?.get("error")?.asDouble()
        val dateTimeStr = jsonItem.get("dateTime")?.asText()
        val dateTime: LocalDateTime = LocalDateTime.parse(dateTimeStr ?: "", getDateTimeFormatter())

        return if (value != null && error != null) {
            RestingHeartRate(value, error, dateTime, profile!!)
        } else {
            null
        }
    }
}

@Component
class DailyHeartRateVariabilityImporterImpl(
    repository: DailyHeartRateVariabilityRepository,
    entityManager: EntityManager,
    transactionManager: PlatformTransactionManager
) : CsvImporter<DailyHeartRateVariability>(repository, entityManager, transactionManager), DailyHeartRateVariabilityImporter {

    override val batchSize: Int = 10000

    override fun entityDate(entity: DailyHeartRateVariability): LocalDate = entity.timestamp.toLocalDate()

    override fun getDateTimeFormatter(): DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

    override fun parseRow(values: List<String>, headers: List<String>): DailyHeartRateVariability? {
        if (values.size < 4) return null

        val timestampStr = values[0]
        val rmssd = values[1].toDoubleOrNull() ?: 0.0
        val nremhr = values[2].toDoubleOrNull() ?: 0.0
        val entropy = values[3].toDoubleOrNull() ?: 0.0
        val timestamp = LocalDateTime.parse(timestampStr, getDateTimeFormatter())

        return DailyHeartRateVariability(
            timestamp = timestamp,
            rmssd = rmssd,
            nremhr = nremhr,
            entropy = entropy,
            profile = profile!!
        )
    }
}

@Component
class HeartRateVariabilityDetailsImporterImpl(
    repository: HeartRateVariabilityDetailsRepository,
    entityManager: EntityManager,
    transactionManager: PlatformTransactionManager
) : CsvImporter<HeartRateVariabilityDetails>(repository, entityManager, transactionManager), HeartRateVariabilityDetailsImporter {

    override val batchSize: Int = 10000

    override fun entityDate(entity: HeartRateVariabilityDetails): LocalDate = entity.timestamp.toLocalDate()

    override fun getDateTimeFormatter(): DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

    override fun parseRow(values: List<String>, headers: List<String>): HeartRateVariabilityDetails? {
        if (values.size < 5) return null

        val timestampStr = values[0]
        val rmssd = values[1].toDoubleOrNull() ?: 0.0
        val coverage = values[2].toDoubleOrNull() ?: 0.0
        val lowFrequency = values[3].toDoubleOrNull() ?: 0.0
        val highFrequency = values[4].toDoubleOrNull() ?: 0.0
        val timestamp = LocalDateTime.parse(timestampStr, getDateTimeFormatter())

        return HeartRateVariabilityDetails(
            timestamp = timestamp,
            rmssd = rmssd,
            coverage = coverage,
            lowFrequency = lowFrequency,
            highFrequency = highFrequency,
            profile = profile!!
        )
    }
}
