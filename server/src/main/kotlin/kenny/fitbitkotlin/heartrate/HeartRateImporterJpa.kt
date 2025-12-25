package kenny.fitbitkotlin.heartrate

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.*
import org.springframework.stereotype.Component
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Component
class HeartRateImporterImpl(
    val repository: HeartRateRepository,
    val batchService: kenny.fitbitkotlin.TransactionalBatchService
): HeartRateImporter {

    override fun parseToEntity(jsonItem: JsonNode): HeartRate? {
        val bpm = jsonItem.get("value")?.get("bpm")?.asInt()
        val confidence = jsonItem.get("value")?.get("confidence")?.asInt()
        val dateTimeStr = jsonItem.get("dateTime")?.asText()
        val dateTime: LocalDateTime = LocalDateTime.parse(dateTimeStr ?: "", getDateTimeFormatter())

        return if (bpm != null && confidence != null) {
            HeartRate(bpm, confidence, dateTime)
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

@Component
class RestingHeartRateImporterImpl(
    val repository: RestingHeartRateRepository,
    val batchService: kenny.fitbitkotlin.TransactionalBatchService
): RestingHeartRateImporter {

    override fun parseToEntity(jsonItem: JsonNode): RestingHeartRate? {
        val valueNode = jsonItem.get("value")
        val value = valueNode?.get("value")?.asDouble()
        val error = valueNode?.get("error")?.asDouble()
        val dateTimeStr = jsonItem.get("dateTime")?.asText()
        val dateTime: LocalDateTime = LocalDateTime.parse(dateTimeStr ?: "", getDateTimeFormatter())

        return if (value != null && error != null) {
            RestingHeartRate(value, error, dateTime)
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

@Component
class DailyHeartRateVariabilityImporterImpl(
    val repository: DailyHeartRateVariabilityRepository,
    val batchService: kenny.fitbitkotlin.TransactionalBatchService
): DailyHeartRateVariabilityImporter {
    // Define a formatter for the ISO date-time format in the CSV
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

    override val batchSize: Int = kenny.fitbitkotlin.BatchConstants.LARGE_BATCH_SIZE

    override fun parseAndSaveEntity(jsonItem: JsonNode) {
        // This method is not used for CSV files, but must be implemented
        throw UnsupportedOperationException("This method is not used for CSV files")
    }

    override fun import(): Int {
        val files = files()
        val size = files.size
        val semaphore = kotlinx.coroutines.sync.Semaphore(maxConcurrentFiles)

        runBlocking(Dispatchers.IO) {
            val jobs = files.mapIndexed { index, file ->
                launch(Dispatchers.IO) {
                    semaphore.acquire()
                    try {
                        importFile(index, size, file)
                        file.renameTo(File(file.absolutePath + ".imported"))
                    } finally {
                        semaphore.release()
                    }
                }
            }
            jobs.joinAll()
        }

        println("Completed processing ${files.size} files")
        return size
    }

    override suspend fun importFile(index: Int, size: Int, file: File) {
        println("Processing file ${index + 1} of $size (%.4f%%".format(100.0 * index / size))
        println("Parsing $file")

        try {
            val batch = mutableListOf<DailyHeartRateVariability>()
            var lineCount = 0

            BufferedReader(FileReader(file)).use { reader ->
                var line = reader.readLine() // Skip header

                while (reader.readLine()?.also { line = it } != null) {
                    val parts = line.split(",")
                    if (parts.size == 4) {
                        val timestampStr = parts[0]
                        val rmssd = parts[1].toDoubleOrNull() ?: 0.0
                        val nremhr = parts[2].toDoubleOrNull() ?: 0.0
                        val entropy = parts[3].toDoubleOrNull() ?: 0.0
                        val timestamp = LocalDateTime.parse(timestampStr, dateTimeFormatter)

                        val dailyHeartRateVariability = DailyHeartRateVariability(
                            timestamp = timestamp,
                            rmssd = rmssd,
                            nremhr = nremhr,
                            entropy = entropy
                        )
                        batch.add(dailyHeartRateVariability)
                        lineCount++

                        if (batch.size >= batchSize) {
                            batchService.saveBatchWithFlush(batch.toList()) { repository.saveAll(it) }
                            batch.clear()
                        }
                    }
                }
            }

            if (batch.isNotEmpty()) {
                batchService.saveBatchWithFlush(batch.toList()) { repository.saveAll(it) }
            }

            println("Imported $lineCount records from ${file.name}")
        } catch (e: Exception) {
            println("Error parsing file ${file.name}: ${e.message}")
            e.printStackTrace()
        }
    }
}

@Component
class HeartRateVariabilityDetailsImporterImpl(
    val repository: HeartRateVariabilityDetailsRepository,
    val batchService: kenny.fitbitkotlin.TransactionalBatchService
): HeartRateVariabilityDetailsImporter {
    // Define a formatter for the ISO date-time format in the CSV
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

    override val batchSize: Int = kenny.fitbitkotlin.BatchConstants.LARGE_BATCH_SIZE

    override fun parseAndSaveEntity(jsonItem: JsonNode) {
        // This method is not used for CSV files, but must be implemented
        throw UnsupportedOperationException("This method is not used for CSV files")
    }

    override fun import(): Int {
        val files = files()
        val size = files.size
        val semaphore = kotlinx.coroutines.sync.Semaphore(maxConcurrentFiles)

        runBlocking(Dispatchers.IO) {
            val jobs = files.mapIndexed { index, file ->
                launch(Dispatchers.IO) {
                    semaphore.acquire()
                    try {
                        importFile(index, size, file)
                        file.renameTo(File(file.absolutePath + ".imported"))
                    } finally {
                        semaphore.release()
                    }
                }
            }
            jobs.joinAll()
        }

        println("Completed processing ${files.size} files")
        return size
    }

    override suspend fun importFile(index: Int, size: Int, file: File) {
        println("Processing file ${index + 1} of $size (%.4f%%".format(100.0 * index / size))
        println("Parsing $file")

        try {
            val batch = mutableListOf<HeartRateVariabilityDetails>()
            var lineCount = 0

            BufferedReader(FileReader(file)).use { reader ->
                var line = reader.readLine() // Skip header

                while (reader.readLine()?.also { line = it } != null) {
                    val parts = line.split(",")
                    if (parts.size == 5) {
                        val timestampStr = parts[0]
                        val rmssd = parts[1].toDoubleOrNull() ?: 0.0
                        val coverage = parts[2].toDoubleOrNull() ?: 0.0
                        val lowFrequency = parts[3].toDoubleOrNull() ?: 0.0
                        val highFrequency = parts[4].toDoubleOrNull() ?: 0.0
                        val timestamp = LocalDateTime.parse(timestampStr, dateTimeFormatter)

                        val heartRateVariabilityDetails = HeartRateVariabilityDetails(
                            timestamp = timestamp,
                            rmssd = rmssd,
                            coverage = coverage,
                            lowFrequency = lowFrequency,
                            highFrequency = highFrequency
                        )
                        batch.add(heartRateVariabilityDetails)
                        lineCount++

                        if (batch.size >= batchSize) {
                            batchService.saveBatchWithFlush(batch.toList()) { repository.saveAll(it) }
                            batch.clear()
                        }
                    }
                }
            }

            if (batch.isNotEmpty()) {
                batchService.saveBatchWithFlush(batch.toList()) { repository.saveAll(it) }
            }

            println("Imported $lineCount records from ${file.name}")
        } catch (e: Exception) {
            println("Error parsing file ${file.name}: ${e.message}")
            e.printStackTrace()
        }
    }
}