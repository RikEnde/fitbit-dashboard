package kenny.fitbit.sleep

import com.fasterxml.jackson.databind.JsonNode
import jakarta.persistence.EntityManager
import kenny.fitbit.CsvImporter
import kenny.fitbit.JsonImporter
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder

@Component
class SleepImporterImpl(
    repository: SleepRepository,
    entityManager: EntityManager,
    transactionManager: PlatformTransactionManager
) : JsonImporter<Sleep>(repository, entityManager, transactionManager), SleepImporter {

    // Cast to specific repository type for findAllLogIds()
    private val sleepRepository: SleepRepository = repository

    override val batchSize: Int = 1000  // Smaller batch for cascade entities

    // Cache of existing log IDs to avoid per-record database lookups
    private var existingLogIds: Set<Long> = emptySet()

    override fun beforeImport() {
        println("Loading existing sleep log IDs for duplicate detection...")
        existingLogIds = sleepRepository.findAllLogIds()
        println("Found ${existingLogIds.size} existing sleep records")
    }

    override fun parseToEntity(jsonItem: JsonNode): Sleep? {
        val logId = jsonItem.get("logId")?.asLong() ?: return null

        // Skip if already imported (check against cached set)
        if (logId in existingLogIds) {
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
}

@Component
class SleepScoreImporterImpl(
    repository: SleepScoreRepository,
    entityManager: EntityManager,
    transactionManager: PlatformTransactionManager
) : CsvImporter<SleepScore>(repository, entityManager, transactionManager), SleepScoreImporter {

    override val batchSize: Int = 10000

    override fun getDateTimeFormatter(): DateTimeFormatter =
        DateTimeFormatter.ofPattern("MM/dd/yy HH:mm:ss")

    override fun parseRow(values: List<String>, headers: List<String>): SleepScore? {
        if (values.size < 9) return null

        val timestampStr = values[0]
        val sleepLogEntryId = values[1].toLongOrNull() ?: return null
        val overallScore = values[2].toIntOrNull() ?: 0
        val compositionScore = values[3].toIntOrNull()
        val revitalizationScore = values[4].toIntOrNull() ?: 0
        val durationScore = values[5].toIntOrNull()
        val deepSleepInMinutes = values[6].toIntOrNull() ?: 0
        val restingHeartRate = values[7].toIntOrNull() ?: 0
        val restlessness = values[8].toDoubleOrNull() ?: 0.0

        val timestamp = LocalDateTime.parse(timestampStr, getDateTimeFormatter())

        return SleepScore(
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
    }
}

@Component
class DeviceTemperatureImporterImpl(
    repository: DeviceTemperatureRepository,
    entityManager: EntityManager,
    transactionManager: PlatformTransactionManager
) : CsvImporter<DeviceTemperature>(repository, entityManager, transactionManager), DeviceTemperatureImporter {

    override val batchSize: Int = 10000

    override fun getDateTimeFormatter(): DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")

    override fun parseRow(values: List<String>, headers: List<String>): DeviceTemperature? {
        if (values.size < 3) return null

        val recordedTimeStr = values[0]
        val temperature = values[1].toDoubleOrNull() ?: 0.0
        val sensorType = values[2]

        val recordedTime = LocalDateTime.parse(recordedTimeStr, getDateTimeFormatter())

        return DeviceTemperature(
            recordedTime = recordedTime,
            temperature = temperature,
            sensorType = sensorType
        )
    }
}

@Component
class DailyRespiratoryRateImporterImpl(
    repository: DailyRespiratoryRateRepository,
    entityManager: EntityManager,
    transactionManager: PlatformTransactionManager
) : CsvImporter<DailyRespiratoryRate>(repository, entityManager, transactionManager), DailyRespiratoryRateImporter {

    override val batchSize: Int = 10000

    override fun getDateTimeFormatter(): DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

    override fun parseRow(values: List<String>, headers: List<String>): DailyRespiratoryRate? {
        if (values.size < 2) return null

        val timestampStr = values[0]
        val dailyRespiratoryRate = values[1].toDoubleOrNull() ?: 0.0

        val timestamp = LocalDateTime.parse(timestampStr, getDateTimeFormatter())

        return DailyRespiratoryRate(
            timestamp = timestamp,
            dailyRespiratoryRate = dailyRespiratoryRate
        )
    }
}

@Component
class MinuteSpO2ImporterImpl(
    repository: MinuteSpO2Repository,
    entityManager: EntityManager,
    transactionManager: PlatformTransactionManager
) : CsvImporter<MinuteSpO2>(repository, entityManager, transactionManager), MinuteSpO2Importer {

    override val batchSize: Int = 10000

    override fun getDateTimeFormatter(): DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")

    override fun parseRow(values: List<String>, headers: List<String>): MinuteSpO2? {
        if (values.size < 2) return null

        val timestampStr = values[0]
        val value = values[1].toDoubleOrNull() ?: 0.0

        val timestamp = LocalDateTime.parse(timestampStr, getDateTimeFormatter())

        return MinuteSpO2(
            timestamp = timestamp,
            value = value
        )
    }
}

@Component
class ComputedTemperatureImporterImpl(
    repository: ComputedTemperatureRepository,
    entityManager: EntityManager,
    transactionManager: PlatformTransactionManager
) : CsvImporter<ComputedTemperature>(repository, entityManager, transactionManager), ComputedTemperatureImporter {

    override val batchSize: Int = 10000

    override fun getDateTimeFormatter(): DateTimeFormatter =
        DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd'T'HH:mm")
            .optionalStart()
            .appendPattern(":ss")
            .optionalEnd()
            .toFormatter()

    override fun parseRow(values: List<String>, headers: List<String>): ComputedTemperature? {
        if (values.size < 9) return null

        val type = values[0]
        val sleepStartStr = values[1]
        val sleepEndStr = values[2]
        val temperatureSamples = values[3].toIntOrNull() ?: 0
        val nightlyTemperature = values[4].toDoubleOrNull() ?: 0.0
        val baselineRelativeSampleSum = values[5].toDoubleOrNull() ?: 0.0
        val baselineRelativeSampleSumOfSquares = values[6].toDoubleOrNull() ?: 0.0
        val baselineRelativeNightlyStandardDeviation = values[7].toDoubleOrNull() ?: 0.0
        val baselineRelativeSampleStandardDeviation = values[8].toDoubleOrNull() ?: 0.0

        val sleepStart = LocalDateTime.parse(sleepStartStr, getDateTimeFormatter())
        val sleepEnd = LocalDateTime.parse(sleepEndStr, getDateTimeFormatter())

        return ComputedTemperature(
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
    }
}

@Component
class DailySpO2ImporterImpl(
    repository: DailySpO2Repository,
    entityManager: EntityManager,
    transactionManager: PlatformTransactionManager
) : CsvImporter<DailySpO2>(repository, entityManager, transactionManager), DailySpO2Importer {

    override val batchSize: Int = 10000

    override fun getDateTimeFormatter(): DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")

    override fun parseRow(values: List<String>, headers: List<String>): DailySpO2? {
        if (values.size < 4) return null

        val timestampStr = values[0]
        val averageValue = values[1].toDoubleOrNull() ?: 0.0
        val lowerBound = values[2].toDoubleOrNull() ?: 0.0
        val upperBound = values[3].toDoubleOrNull() ?: 0.0

        val timestamp = LocalDateTime.parse(timestampStr, getDateTimeFormatter())

        return DailySpO2(
            timestamp = timestamp,
            averageValue = averageValue,
            lowerBound = lowerBound,
            upperBound = upperBound
        )
    }
}

@Component
class RespiratoryRateSummaryImporterImpl(
    repository: RespiratoryRateSummaryRepository,
    entityManager: EntityManager,
    transactionManager: PlatformTransactionManager
) : CsvImporter<RespiratoryRateSummary>(repository, entityManager, transactionManager), RespiratoryRateSummaryImporter {

    override val batchSize: Int = 10000

    override fun getDateTimeFormatter(): DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

    override fun parseRow(values: List<String>, headers: List<String>): RespiratoryRateSummary? {
        if (values.size < 13) return null

        val timestampStr = values[0]
        val fullSleepBreathingRate = values[1].toDoubleOrNull() ?: 0.0
        val fullSleepStandardDeviation = values[2].toDoubleOrNull() ?: 0.0
        val fullSleepSignalToNoise = values[3].toDoubleOrNull() ?: 0.0
        val deepSleepBreathingRate = values[4].toDoubleOrNull() ?: 0.0
        val deepSleepStandardDeviation = values[5].toDoubleOrNull() ?: 0.0
        val deepSleepSignalToNoise = values[6].toDoubleOrNull() ?: 0.0
        val lightSleepBreathingRate = values[7].toDoubleOrNull() ?: 0.0
        val lightSleepStandardDeviation = values[8].toDoubleOrNull() ?: 0.0
        val lightSleepSignalToNoise = values[9].toDoubleOrNull() ?: 0.0
        val remSleepBreathingRate = values[10].toDoubleOrNull() ?: -1.0
        val remSleepStandardDeviation = values[11].toDoubleOrNull() ?: 0.0
        val remSleepSignalToNoise = values[12].toDoubleOrNull() ?: 0.0

        val timestamp = LocalDateTime.parse(timestampStr, getDateTimeFormatter())

        return RespiratoryRateSummary(
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
    }
}
