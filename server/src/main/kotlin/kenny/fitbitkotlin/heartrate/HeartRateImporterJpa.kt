package kenny.fitbitkotlin.heartrate

import com.fasterxml.jackson.databind.JsonNode
import kotlinx.coroutines.*
import org.springframework.stereotype.Component
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Component
class HeartRateImporterImpl(val repository: HeartRateRepository): HeartRateImporter {

    override fun parseAndSaveEntity(jsonItem: JsonNode) {
        val bpm = jsonItem.get("value")?.get("bpm")?.asInt()
        val confidence = jsonItem.get("value")?.get("confidence")?.asInt()
        val dateTimeStr = jsonItem.get("dateTime")?.asText()
        val dateTime: LocalDateTime = LocalDateTime.parse(dateTimeStr ?: "", getDateTimeFormatter())

        if (bpm != null && confidence != null) {
            val heartRate = HeartRate(bpm, confidence, dateTime)
            repository.save(heartRate)
        }
    }
}

@Component
class RestingHeartRateImporterImpl(val repository: RestingHeartRateRepository): RestingHeartRateImporter {

    override fun parseAndSaveEntity(jsonItem: JsonNode) {
        val valueNode = jsonItem.get("value")
        val value = valueNode?.get("value")?.asDouble()
        val error = valueNode?.get("error")?.asDouble()
        val dateTimeStr = jsonItem.get("dateTime")?.asText()
        val dateTime: LocalDateTime = LocalDateTime.parse(dateTimeStr ?: "", getDateTimeFormatter())

        if (value != null && error != null) {
            val restingHeartRate = RestingHeartRate(value, error, dateTime)
            repository.save(restingHeartRate)
        }
    }
}

@Component
class DailyHeartRateVariabilityImporterImpl(val repository: DailyHeartRateVariabilityRepository): DailyHeartRateVariabilityImporter {
    // Define a formatter for the ISO date-time format in the CSV
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

    override fun parseAndSaveEntity(jsonItem: JsonNode) {
        // This method is not used for CSV files, but must be implemented
        throw UnsupportedOperationException("This method is not used for CSV files")
    }

    override fun import(): Int {
        val files = files()
        val size = files.size

        runBlocking(Dispatchers.IO) {
            val jobs = files.mapIndexed { index, file ->
                launch(Dispatchers.IO) {
                    importFile(index, size, file)
                    file.renameTo(File(file.absolutePath + ".imported"))
                }
            }
            jobs.joinAll()
        }

        println("Completed processing ${files.size} files")
        return size
    }

    override suspend fun importFile(index: Int, size: Int, file: File) {
        println("Processing file ${index + 1} of $size (%.4f".format(100.0 * index / size))
        println("Parsing $file")

        try {
            // Skip the header line
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

                        // Parse the date time using the formatter
                        val timestamp = LocalDateTime.parse(timestampStr, dateTimeFormatter)

                        // Create and save the DailyHeartRateVariability entity
                        val dailyHeartRateVariability = DailyHeartRateVariability(
                            timestamp = timestamp,
                            rmssd = rmssd,
                            nremhr = nremhr,
                            entropy = entropy
                        )
                        repository.save(dailyHeartRateVariability)
                        lineCount++
                    }
                }
            }

            println("Imported $lineCount records from ${file.name}")
        } catch (e: Exception) {
            println("Error parsing file ${file.name}: ${e.message}")
            e.printStackTrace()
        }
    }
}

@Component
class HeartRateVariabilityDetailsImporterImpl(val repository: HeartRateVariabilityDetailsRepository): HeartRateVariabilityDetailsImporter {
    // Define a formatter for the ISO date-time format in the CSV
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

    override fun parseAndSaveEntity(jsonItem: JsonNode) {
        // This method is not used for CSV files, but must be implemented
        throw UnsupportedOperationException("This method is not used for CSV files")
    }

    override fun import(): Int {
        val files = files()
        val size = files.size

        runBlocking(Dispatchers.IO) {
            val jobs = files.mapIndexed { index, file ->
                launch(Dispatchers.IO) {
                    importFile(index, size, file)
                    file.renameTo(File(file.absolutePath + ".imported"))
                }
            }
            jobs.joinAll()
        }

        println("Completed processing ${files.size} files")
        return size
    }

    override suspend fun importFile(index: Int, size: Int, file: File) {
        println("Processing file ${index + 1} of $size (%.4f".format(100.0 * index / size))
        println("Parsing $file")

        try {
            // Skip the header line
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

                        // Parse the date time using the formatter
                        val timestamp = LocalDateTime.parse(timestampStr, dateTimeFormatter)

                        // Create and save the HeartRateVariabilityDetails entity
                        val heartRateVariabilityDetails = HeartRateVariabilityDetails(
                            timestamp = timestamp,
                            rmssd = rmssd,
                            coverage = coverage,
                            lowFrequency = lowFrequency,
                            highFrequency = highFrequency
                        )
                        repository.save(heartRateVariabilityDetails)
                        lineCount++
                    }
                }
            }

            println("Imported $lineCount records from ${file.name}")
        } catch (e: Exception) {
            println("Error parsing file ${file.name}: ${e.message}")
            e.printStackTrace()
        }
    }
}