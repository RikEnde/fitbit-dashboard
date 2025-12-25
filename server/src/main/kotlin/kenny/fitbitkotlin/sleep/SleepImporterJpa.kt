package kenny.fitbitkotlin.sleep

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.*
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Component
class SleepImporterImpl(
    val repository: SleepRepository,
    val sleepLevelSummaryRepository: SleepLevelSummaryRepository,
    val sleepLevelDataRepository: SleepLevelDataRepository,
    val sleepLevelShortDataRepository: SleepLevelShortDataRepository,
    val batchService: kenny.fitbitkotlin.TransactionalBatchService
) : SleepImporter {

    override val batchSize: Int = kenny.fitbitkotlin.BatchConstants.SMALL_BATCH_SIZE

    override fun parseToEntity(jsonItem: JsonNode): Sleep? {
        val logId = jsonItem.get("logId")?.asLong() ?: return null

        // Skip if already imported
        if (repository.findByLogId(logId) != null) {
            return null
        }

        val dateOfSleepStr = jsonItem.get("dateOfSleep")?.asText()
        val startTimeStr = jsonItem.get("startTime")?.asText()
        val endTimeStr = jsonItem.get("endTime")?.asText()
        val duration = jsonItem.get("duration")?.asLong() ?: 0
        val minutesToFallAsleep = jsonItem.get("minutesToFallAsleep")?.asInt() ?: 0
        val minutesAsleep = jsonItem.get("minutesAsleep")?.asInt() ?: 0
        val minutesAwake = jsonItem.get("minutesAwake")?.asInt() ?: 0
        val minutesAfterWakeup = jsonItem.get("minutesAfterWakeup")?.asInt() ?: 0
        val timeInBed = jsonItem.get("timeInBed")?.asInt() ?: 0
        val efficiency = jsonItem.get("efficiency")?.asInt() ?: 0
        val type = jsonItem.get("type")?.asText() ?: ""
        val infoCode = jsonItem.get("infoCode")?.asInt() ?: 0
        val logType = jsonItem.get("logType")?.asText() ?: ""
        val mainSleep = jsonItem.get("mainSleep")?.asBoolean() ?: false

        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS")
        val dateOfSleep = LocalDate.parse(dateOfSleepStr, dateFormatter).atStartOfDay()
        val startTime = LocalDateTime.parse(startTimeStr, dateTimeFormatter)
        val endTime = LocalDateTime.parse(endTimeStr, dateTimeFormatter)

        val sleep = Sleep(
            logId = logId,
            dateOfSleep = dateOfSleep,
            startTime = startTime,
            endTime = endTime,
            duration = duration,
            minutesToFallAsleep = minutesToFallAsleep,
            minutesAsleep = minutesAsleep,
            minutesAwake = minutesAwake,
            minutesAfterWakeup = minutesAfterWakeup,
            timeInBed = timeInBed,
            efficiency = efficiency,
            type = type,
            infoCode = infoCode,
            logType = logType,
            mainSleep = mainSleep
        )

        // Process level summaries
        val levelSummaries = jsonItem.get("levels")?.get("summary")
        if (levelSummaries != null) {
            for (field in levelSummaries.fieldNames()) {
                val summary = levelSummaries.get(field)
                val count = summary.get("count")?.asInt() ?: 0
                val minutes = summary.get("minutes")?.asInt() ?: 0
                val thirtyDayAvgMinutes = summary.get("thirtyDayAvgMinutes")?.asInt() ?: 0

                val sleepLevelSummary = SleepLevelSummary(
                    level = field,
                    count = count,
                    minutes = minutes,
                    thirtyDayAvgMinutes = thirtyDayAvgMinutes,
                    sleep = sleep
                )
                sleep.levelSummaries.add(sleepLevelSummary)
            }
        }

        // Process level data
        val levelData = jsonItem.get("levels")?.get("data")
        if (levelData != null && levelData.isArray) {
            for (data in levelData) {
                val dateTimeStr = data.get("dateTime")?.asText()
                val level = data.get("level")?.asText() ?: ""
                val seconds = data.get("seconds")?.asInt() ?: 0

                val dateTime = LocalDateTime.parse(dateTimeStr, dateTimeFormatter)

                val sleepLevelData = SleepLevelData(
                    dateTime = dateTime,
                    level = level,
                    seconds = seconds,
                    sleep = sleep
                )
                sleep.levelData.add(sleepLevelData)
            }
        }

        // Process level short data
        val levelShortData = jsonItem.get("levels")?.get("shortData")
        if (levelShortData != null && levelShortData.isArray) {
            for (data in levelShortData) {
                val dateTimeStr = data.get("dateTime")?.asText()
                val level = data.get("level")?.asText() ?: ""
                val seconds = data.get("seconds")?.asInt() ?: 0

                val dateTime = LocalDateTime.parse(dateTimeStr, dateTimeFormatter)

                val sleepLevelShortData = SleepLevelShortData(
                    dateTime = dateTime,
                    level = level,
                    seconds = seconds,
                    sleep = sleep
                )
                sleep.levelShortData.add(sleepLevelShortData)
            }
        }

        return sleep
    }

    override fun parseAndSaveEntity(jsonItem: JsonNode) {
        // Not used - parseToEntity handles it
    }

    @Transactional(propagation = Propagation.REQUIRED)
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
class SleepScoreImporterImpl(
    val repository: SleepScoreRepository,
    val batchService: kenny.fitbitkotlin.TransactionalBatchService
): SleepScoreImporter {
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("MM/dd/yy HH:mm:ss")

    override val batchSize: Int = kenny.fitbitkotlin.BatchConstants.LARGE_BATCH_SIZE

    override fun parseAndSaveEntity(jsonItem: JsonNode) {
        // This method is not used for CSV imports
        // The import and importFile methods are used instead
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

    @Transactional(propagation = Propagation.REQUIRED)
    override suspend fun importFile(index: Int, size: Int, file: File) {
        println("Processing file ${index + 1} of $size (%.4f%%".format(100.0 * index / size))
        println("Parsing $file")

        try {
            val batch = mutableListOf<SleepScore>()
            var lineCount = 0

            BufferedReader(FileReader(file)).use { reader ->
                reader.readLine() // Skip header

                var line: String? = null
                while (reader.readLine()?.also { line = it } != null) {
                    val parts = line!!.split(",")
                    if (parts.size >= 9) {
                        val timestampStr = parts[0]
                        val sleepLogEntryId = parts[1].toLongOrNull() ?: continue
                        val overallScore = parts[2].toIntOrNull() ?: 0
                        val compositionScore = parts[3].toIntOrNull()
                        val revitalizationScore = parts[4].toIntOrNull() ?: 0
                        val durationScore = parts[5].toIntOrNull()
                        val deepSleepInMinutes = parts[6].toIntOrNull() ?: 0
                        val restingHeartRate = parts[7].toIntOrNull() ?: 0
                        val restlessness = parts[8].toDoubleOrNull() ?: 0.0

                        // Parse the date time using the formatter
                        val timestamp = LocalDateTime.parse(timestampStr, dateTimeFormatter)

                        // Create the SleepScore entity
                        val sleepScore = SleepScore(
                            sleepLogEntryId = sleepLogEntryId,
                            timestamp = timestamp,
                            overallScore = overallScore,
                            compositionScore = compositionScore,
                            revitalizationScore = revitalizationScore,
                            durationScore = durationScore,
                            deepSleepInMinutes = deepSleepInMinutes,
                            restingHeartRate = restingHeartRate,
                            restlessness = restlessness
                        )
                        batch.add(sleepScore)
                        lineCount++

                        // Batch flush
                        if (batch.size >= batchSize) {
                            batchService.saveBatchWithFlush(batch.toList()) { repository.saveAll(it) }
                            batch.clear()
                        }
                    }
                }
            }

            // Save remaining
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
class DeviceTemperatureImporterImpl(
    val repository: DeviceTemperatureRepository,
    val batchService: kenny.fitbitkotlin.TransactionalBatchService
): DeviceTemperatureImporter {
    // Define a formatter for the ISO date-time format in the CSV
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")

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

    @Transactional(propagation = Propagation.REQUIRED)
    override suspend fun importFile(index: Int, size: Int, file: File) {
        println("Processing file ${index + 1} of $size (%.4f%%".format(100.0 * index / size))
        println("Parsing $file")

        try {
            val batch = mutableListOf<DeviceTemperature>()
            var lineCount = 0

            BufferedReader(FileReader(file)).use { reader ->
                reader.readLine() // Skip header

                var line: String? = null
                while (reader.readLine()?.also { line = it } != null) {
                    val parts = line!!.split(",")
                    if (parts.size == 3) {
                        val recordedTimeStr = parts[0]
                        val temperature = parts[1].toDoubleOrNull() ?: 0.0
                        val sensorType = parts[2]

                        // Parse the date time using the formatter
                        val recordedTime = LocalDateTime.parse(recordedTimeStr, dateTimeFormatter)

                        // Create the DeviceTemperature entity
                        val deviceTemperature = DeviceTemperature(
                            recordedTime = recordedTime,
                            temperature = temperature,
                            sensorType = sensorType
                        )
                        batch.add(deviceTemperature)
                        lineCount++

                        // Batch flush
                        if (batch.size >= batchSize) {
                            batchService.saveBatchWithFlush(batch.toList()) { repository.saveAll(it) }
                            batch.clear()
                        }
                    }
                }
            }

            // Save remaining
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
class DailyRespiratoryRateImporterImpl(
    val repository: DailyRespiratoryRateRepository,
    val batchService: kenny.fitbitkotlin.TransactionalBatchService
): DailyRespiratoryRateImporter {
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

    @Transactional(propagation = Propagation.REQUIRED)
    override suspend fun importFile(index: Int, size: Int, file: File) {
        println("Processing file ${index + 1} of $size (%.4f%%".format(100.0 * index / size))
        println("Parsing $file")

        try {
            val batch = mutableListOf<DailyRespiratoryRate>()
            var lineCount = 0

            BufferedReader(FileReader(file)).use { reader ->
                reader.readLine() // Skip header

                var line: String? = null
                while (reader.readLine()?.also { line = it } != null) {
                    val parts = line!!.split(",")
                    if (parts.size == 2) {
                        val timestampStr = parts[0]
                        val dailyRespiratoryRate = parts[1].toDoubleOrNull() ?: 0.0

                        // Parse the date time using the formatter
                        val timestamp = LocalDateTime.parse(timestampStr, dateTimeFormatter)

                        // Create the DailyRespiratoryRate entity
                        val dailyRespiratoryRateEntity = DailyRespiratoryRate(
                            timestamp = timestamp,
                            dailyRespiratoryRate = dailyRespiratoryRate
                        )
                        batch.add(dailyRespiratoryRateEntity)
                        lineCount++

                        // Batch flush
                        if (batch.size >= batchSize) {
                            batchService.saveBatchWithFlush(batch.toList()) { repository.saveAll(it) }
                            batch.clear()
                        }
                    }
                }
            }

            // Save remaining
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
class MinuteSpO2ImporterImpl(
    val repository: MinuteSpO2Repository,
    val batchService: kenny.fitbitkotlin.TransactionalBatchService
): MinuteSpO2Importer {
    // Define a formatter for the ISO date-time format in the CSV
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")

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

    @Transactional(propagation = Propagation.REQUIRED)
    override suspend fun importFile(index: Int, size: Int, file: File) {
        println("Processing file ${index + 1} of $size (%.4f%%".format(100.0 * index / size))
        println("Parsing $file")

        try {
            val batch = mutableListOf<MinuteSpO2>()
            var lineCount = 0

            BufferedReader(FileReader(file)).use { reader ->
                reader.readLine() // Skip header

                var line: String? = null
                while (reader.readLine()?.also { line = it } != null) {
                    val parts = line!!.split(",")
                    if (parts.size == 2) {
                        val timestampStr = parts[0]
                        val value = parts[1].toDoubleOrNull() ?: 0.0

                        // Parse the date time using the formatter
                        val timestamp = LocalDateTime.parse(timestampStr, dateTimeFormatter)

                        // Create the MinuteSpO2 entity
                        val minuteSpO2 = MinuteSpO2(
                            timestamp = timestamp,
                            value = value
                        )
                        batch.add(minuteSpO2)
                        lineCount++

                        // Batch flush
                        if (batch.size >= batchSize) {
                            batchService.saveBatchWithFlush(batch.toList()) { repository.saveAll(it) }
                            batch.clear()
                        }
                    }
                }
            }

            // Save remaining
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
class ComputedTemperatureImporterImpl(
    val repository: ComputedTemperatureRepository,
    val batchService: kenny.fitbitkotlin.TransactionalBatchService
): ComputedTemperatureImporter {
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

    @Transactional(propagation = Propagation.REQUIRED)
    override suspend fun importFile(index: Int, size: Int, file: File) {
        println("Processing file ${index + 1} of $size (%.4f%%".format(100.0 * index / size))
        println("Parsing $file")

        try {
            val batch = mutableListOf<ComputedTemperature>()
            var lineCount = 0

            BufferedReader(FileReader(file)).use { reader ->
                reader.readLine() // Skip header

                var line: String? = null
                while (reader.readLine()?.also { line = it } != null) {
                    val parts = line!!.split(",")
                    if (parts.size == 9) {
                        val type = parts[0]
                        val sleepStartStr = parts[1]
                        val sleepEndStr = parts[2]
                        val temperatureSamples = parts[3].toIntOrNull() ?: 0
                        val nightlyTemperature = parts[4].toDoubleOrNull() ?: 0.0
                        val baselineRelativeSampleSum = parts[5].toDoubleOrNull() ?: 0.0
                        val baselineRelativeSampleSumOfSquares = parts[6].toDoubleOrNull() ?: 0.0
                        val baselineRelativeNightlyStandardDeviation = parts[7].toDoubleOrNull() ?: 0.0
                        val baselineRelativeSampleStandardDeviation = parts[8].toDoubleOrNull() ?: 0.0

                        // Parse the date times using the formatter
                        val sleepStart = LocalDateTime.parse(sleepStartStr, dateTimeFormatter)
                        val sleepEnd = LocalDateTime.parse(sleepEndStr, dateTimeFormatter)

                        // Create the ComputedTemperature entity
                        val computedTemperature = ComputedTemperature(
                            type = type,
                            sleepStart = sleepStart,
                            sleepEnd = sleepEnd,
                            temperatureSamples = temperatureSamples,
                            nightlyTemperature = nightlyTemperature,
                            baselineRelativeSampleSum = baselineRelativeSampleSum,
                            baselineRelativeSampleSumOfSquares = baselineRelativeSampleSumOfSquares,
                            baselineRelativeNightlyStandardDeviation = baselineRelativeNightlyStandardDeviation,
                            baselineRelativeSampleStandardDeviation = baselineRelativeSampleStandardDeviation
                        )
                        batch.add(computedTemperature)
                        lineCount++

                        // Batch flush
                        if (batch.size >= batchSize) {
                            batchService.saveBatchWithFlush(batch.toList()) { repository.saveAll(it) }
                            batch.clear()
                        }
                    }
                }
            }

            // Save remaining
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

// Daily SpO2 Importer Implementation
@Component
class DailySpO2ImporterImpl(
    val repository: DailySpO2Repository,
    val batchService: kenny.fitbitkotlin.TransactionalBatchService
): DailySpO2Importer {
    // Define a formatter for the ISO date-time format in the CSV
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")

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

    @Transactional(propagation = Propagation.REQUIRED)
    override suspend fun importFile(index: Int, size: Int, file: File) {
        println("Processing file ${index + 1} of $size (%.4f%%".format(100.0 * index / size))
        println("Parsing $file")

        try {
            val batch = mutableListOf<DailySpO2>()
            var lineCount = 0

            BufferedReader(FileReader(file)).use { reader ->
                reader.readLine() // Skip header

                var line: String? = null
                while (reader.readLine()?.also { line = it } != null) {
                    val parts = line!!.split(",")
                    if (parts.size == 4) {
                        val timestampStr = parts[0]
                        val averageValue = parts[1].toDoubleOrNull() ?: 0.0
                        val lowerBound = parts[2].toDoubleOrNull() ?: 0.0
                        val upperBound = parts[3].toDoubleOrNull() ?: 0.0

                        // Parse the date time using the formatter
                        val timestamp = LocalDateTime.parse(timestampStr, dateTimeFormatter)

                        // Create the DailySpO2 entity
                        val dailySpO2 = DailySpO2(
                            timestamp = timestamp,
                            averageValue = averageValue,
                            lowerBound = lowerBound,
                            upperBound = upperBound
                        )
                        batch.add(dailySpO2)
                        lineCount++

                        // Batch flush
                        if (batch.size >= batchSize) {
                            batchService.saveBatchWithFlush(batch.toList()) { repository.saveAll(it) }
                            batch.clear()
                        }
                    }
                }
            }

            // Save remaining
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
class RespiratoryRateSummaryImporterImpl(
    val repository: RespiratoryRateSummaryRepository,
    val batchService: kenny.fitbitkotlin.TransactionalBatchService
): RespiratoryRateSummaryImporter {
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

    @Transactional(propagation = Propagation.REQUIRED)
    override suspend fun importFile(index: Int, size: Int, file: File) {
        println("Processing file ${index + 1} of $size (%.4f%%".format(100.0 * index / size))
        println("Parsing $file")

        try {
            val batch = mutableListOf<RespiratoryRateSummary>()
            var lineCount = 0

            BufferedReader(FileReader(file)).use { reader ->
                reader.readLine() // Skip header

                var line: String? = null
                while (reader.readLine()?.also { line = it } != null) {
                    val parts = line!!.split(",")
                    if (parts.size == 13) {
                        val timestampStr = parts[0]
                        val fullSleepBreathingRate = parts[1].toDoubleOrNull() ?: 0.0
                        val fullSleepStandardDeviation = parts[2].toDoubleOrNull() ?: 0.0
                        val fullSleepSignalToNoise = parts[3].toDoubleOrNull() ?: 0.0
                        val deepSleepBreathingRate = parts[4].toDoubleOrNull() ?: 0.0
                        val deepSleepStandardDeviation = parts[5].toDoubleOrNull() ?: 0.0
                        val deepSleepSignalToNoise = parts[6].toDoubleOrNull() ?: 0.0
                        val lightSleepBreathingRate = parts[7].toDoubleOrNull() ?: 0.0
                        val lightSleepStandardDeviation = parts[8].toDoubleOrNull() ?: 0.0
                        val lightSleepSignalToNoise = parts[9].toDoubleOrNull() ?: 0.0
                        val remSleepBreathingRate = parts[10].toDoubleOrNull() ?: -1.0
                        val remSleepStandardDeviation = parts[11].toDoubleOrNull() ?: 0.0
                        val remSleepSignalToNoise = parts[12].toDoubleOrNull() ?: 0.0

                        // Parse the date time using the formatter
                        val timestamp = LocalDateTime.parse(timestampStr, dateTimeFormatter)

                        // Create the RespiratoryRateSummary entity
                        val respiratoryRateSummary = RespiratoryRateSummary(
                            timestamp = timestamp,
                            fullSleepBreathingRate = fullSleepBreathingRate,
                            fullSleepStandardDeviation = fullSleepStandardDeviation,
                            fullSleepSignalToNoise = fullSleepSignalToNoise,
                            deepSleepBreathingRate = deepSleepBreathingRate,
                            deepSleepStandardDeviation = deepSleepStandardDeviation,
                            deepSleepSignalToNoise = deepSleepSignalToNoise,
                            lightSleepBreathingRate = lightSleepBreathingRate,
                            lightSleepStandardDeviation = lightSleepStandardDeviation,
                            lightSleepSignalToNoise = lightSleepSignalToNoise,
                            remSleepBreathingRate = remSleepBreathingRate,
                            remSleepStandardDeviation = remSleepStandardDeviation,
                            remSleepSignalToNoise = remSleepSignalToNoise
                        )
                        batch.add(respiratoryRateSummary)
                        lineCount++

                        // Batch flush
                        if (batch.size >= batchSize) {
                            batchService.saveBatchWithFlush(batch.toList()) { repository.saveAll(it) }
                            batch.clear()
                        }
                    }
                }
            }

            // Save remaining
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
