package kenny.fitbit.exercise

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.persistence.EntityManager
import kenny.fitbit.CsvImporter
import kenny.fitbit.JsonImporter
import kenny.fitbit.heartrate.TimeInHeartRateZoneValue
import kenny.fitbit.heartrate.TimeInHeartRateZones
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Component
class ExerciseImporterImpl(
    repository: ExerciseRepository,
    entityManager: EntityManager,
    transactionManager: PlatformTransactionManager
) : JsonImporter<Exercise>(repository, entityManager, transactionManager), ExerciseImporter {

    override val batchSize: Int = 2000

    override fun entityDate(entity: Exercise): LocalDate = entity.startTime.toLocalDate()

    override fun parseToEntity(jsonItem: JsonNode): Exercise? {
        val logId = jsonItem.get("logId")?.asLong() ?: return null
        val activityName = jsonItem.get("activityName")?.asText() ?: ""
        val activityTypeId = jsonItem.get("activityTypeId")?.asInt() ?: 0
        val averageHeartRate = jsonItem.get("averageHeartRate")?.asInt()
        val calories = jsonItem.get("calories")?.asInt() ?: 0
        val duration = jsonItem.get("duration")?.asLong() ?: 0
        val activeDuration = jsonItem.get("activeDuration")?.asLong() ?: 0
        val steps = jsonItem.get("steps")?.asInt()
        val logType = jsonItem.get("logType")?.asText() ?: ""
        val startTimeStr = jsonItem.get("startTime")?.asText()
        val lastModifiedStr = jsonItem.get("lastModified")?.asText()
        val originalStartTimeStr = jsonItem.get("originalStartTime")?.asText()
        val originalDuration = jsonItem.get("originalDuration")?.asLong()
        val elevationGain = jsonItem.get("elevationGain")?.asDouble()
        val hasGps = jsonItem.get("hasGps")?.asBoolean() ?: false
        val shouldFetchDetails = jsonItem.get("shouldFetchDetails")?.asBoolean() ?: false
        val hasActiveZoneMinutes = jsonItem.get("hasActiveZoneMinutes")?.asBoolean() ?: false

        val startTime = LocalDateTime.parse(startTimeStr, getDateTimeFormatter())
        val lastModified = if (lastModifiedStr != null) LocalDateTime.parse(lastModifiedStr, getDateTimeFormatter()) else null
        val originalStartTime = if (originalStartTimeStr != null) LocalDateTime.parse(originalStartTimeStr, getDateTimeFormatter()) else null

        val exercise = Exercise(
            logId = logId,
            activityName = activityName,
            activityTypeId = activityTypeId,
            averageHeartRate = averageHeartRate,
            calories = calories,
            duration = duration,
            activeDuration = activeDuration,
            steps = steps,
            logType = logType,
            startTime = startTime,
            lastModified = lastModified,
            originalStartTime = originalStartTime,
            originalDuration = originalDuration,
            elevationGain = elevationGain,
            hasGps = hasGps,
            shouldFetchDetails = shouldFetchDetails,
            hasActiveZoneMinutes = hasActiveZoneMinutes,
            profile = profile!!
        )

        // Process heart rate zones
        val heartRateZones = jsonItem.get("heartRateZones")
        if (heartRateZones != null && heartRateZones.isArray) {
            for (zone in heartRateZones) {
                val name = zone.get("name")?.asText() ?: ""
                val min = zone.get("min")?.asInt() ?: 0
                val max = zone.get("max")?.asInt() ?: 0
                val minutes = zone.get("minutes")?.asInt() ?: 0

                val heartRateZone = HeartRateZone(
                    name = name,
                    min = min,
                    max = max,
                    minutes = minutes,
                    exercise = exercise
                )
                exercise.heartRateZones.add(heartRateZone)
            }
        }

        // Process activity levels
        val activityLevels = jsonItem.get("activityLevel")
        if (activityLevels != null && activityLevels.isArray) {
            for (level in activityLevels) {
                val name = level.get("name")?.asText() ?: ""
                val minutes = level.get("minutes")?.asInt() ?: 0

                val activityLevel = ActivityLevel(
                    name = name,
                    minutes = minutes,
                    exercise = exercise
                )
                exercise.activityLevels.add(activityLevel)
            }
        }

        return exercise
    }
}

@Component
class ActivityMinutesImporterImpl(
    repository: ActivityMinutesRepository,
    entityManager: EntityManager,
    transactionManager: PlatformTransactionManager
) : JsonImporter<ActivityMinutes>(repository, entityManager, transactionManager), ActivityMinutesImporter {

    override fun entityDate(entity: ActivityMinutes): LocalDate = entity.dateTime.toLocalDate()

    // Track the current file being processed
    private val currentFile = ThreadLocal<String>()

    override suspend fun importFile(index: Int, size: Int, file: File, objectMapper: ObjectMapper) {
        currentFile.set(file.name)
        try {
            super.importFile(index, size, file, objectMapper)
        } finally {
            currentFile.remove()
        }
    }

    override fun parseToEntity(jsonItem: JsonNode): ActivityMinutes? {
        val valueStr = jsonItem.get("value")?.asText()
        val dateTimeStr = jsonItem.get("dateTime")?.asText()

        if (valueStr != null && dateTimeStr != null) {
            val value = valueStr.toInt()
            val dateTime = LocalDateTime.parse(dateTimeStr, getDateTimeFormatter())

            // Get the file name from the ThreadLocal to determine the activity type
            val fileName = currentFile.get() ?: throw IllegalStateException("File name not set in ThreadLocal")
            val intensity = when {
                fileName.contains("sedentary_minutes") -> "sedentary"
                fileName.contains("lightly_active_minutes") -> "light"
                fileName.contains("moderately_active_minutes") -> "moderate"
                fileName.contains("very_active_minutes") -> "active"
                else -> throw IllegalArgumentException("Unknown activity type in file: $fileName")
            }

            return ActivityMinutes(
                dateTime = dateTime,
                value = value,
                intensity = intensity,
                profile = profile!!
            )
        }
        return null
    }
}

@Component
class ActiveZoneMinutesImporterImpl(
    repository: ActivityMinutesRepository,
    entityManager: EntityManager,
    transactionManager: PlatformTransactionManager
) : CsvImporter<ActivityMinutes>(repository, entityManager, transactionManager), ActiveZoneMinutesImporter {

    override val batchSize: Int = 10000

    override fun entityDate(entity: ActivityMinutes): LocalDate = entity.dateTime.toLocalDate()

    override fun getDateTimeFormatter(): DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")

    override fun parseRow(values: List<String>, headers: List<String>): ActivityMinutes? {
        if (values.size < 3) return null

        val dateTimeStr = values[0]
        val heartZoneId = values[1]
        val totalMinutes = values[2].toIntOrNull() ?: 0

        val dateTime = LocalDateTime.parse(dateTimeStr, getDateTimeFormatter())

        // Map heart zone to intensity
        val intensity = when (heartZoneId) {
            "FAT_BURN" -> "moderate"
            "CARDIO" -> "active"
            "PEAK" -> "active"
            else -> "light"
        }

        return ActivityMinutes(
            dateTime = dateTime,
            value = totalMinutes,
            intensity = intensity,
            profile = profile!!
        )
    }
}

@Component
class DemographicVO2MaxImporterImpl(
    repository: DemographicVO2MaxRepository,
    entityManager: EntityManager,
    transactionManager: PlatformTransactionManager
) : JsonImporter<DemographicVO2Max>(repository, entityManager, transactionManager), DemographicVO2MaxImporter {

    override fun entityDate(entity: DemographicVO2Max): LocalDate = entity.dateTime.toLocalDate()

    override fun parseToEntity(jsonItem: JsonNode): DemographicVO2Max? {
        val valueNode = jsonItem.get("value") ?: return null
        val demographicVO2Max = valueNode.get("demographicVO2Max")?.asDouble()
        val demographicVO2MaxError = valueNode.get("demographicVO2MaxError")?.asDouble()
        val filteredDemographicVO2Max = valueNode.get("filteredDemographicVO2Max")?.asDouble()
        val filteredDemographicVO2MaxError = valueNode.get("filteredDemographicVO2MaxError")?.asDouble()
        val dateTimeStr = jsonItem.get("dateTime")?.asText() ?: return null

        val dateTime = LocalDateTime.parse(dateTimeStr, getDateTimeFormatter())

        return if (demographicVO2Max != null && demographicVO2MaxError != null &&
            filteredDemographicVO2Max != null && filteredDemographicVO2MaxError != null) {
            DemographicVO2Max(
                demographicVO2Max = demographicVO2Max,
                demographicVO2MaxError = demographicVO2MaxError,
                filteredDemographicVO2Max = filteredDemographicVO2Max,
                filteredDemographicVO2MaxError = filteredDemographicVO2MaxError,
                dateTime = dateTime,
                profile = profile!!
            )
        } else null
    }
}

@Component
class RunVO2MaxImporterImpl(
    repository: RunVO2MaxRepository,
    entityManager: EntityManager,
    transactionManager: PlatformTransactionManager
) : JsonImporter<RunVO2Max>(repository, entityManager, transactionManager), RunVO2MaxImporter {

    override fun entityDate(entity: RunVO2Max): LocalDate = entity.dateTime.toLocalDate()

    override fun parseToEntity(jsonItem: JsonNode): RunVO2Max? {
        val valueNode = jsonItem.get("value") ?: return null
        val exerciseId = valueNode.get("exerciseId")?.asLong()
        val runVO2Max = valueNode.get("runVO2Max")?.asDouble()
        val runVO2MaxError = valueNode.get("runVO2MaxError")?.asDouble()
        val filteredRunVO2Max = valueNode.get("filteredRunVO2Max")?.asDouble()
        val filteredRunVO2MaxError = valueNode.get("filteredRunVO2MaxError")?.asDouble()
        val dateTimeStr = jsonItem.get("dateTime")?.asText() ?: return null

        val dateTime = LocalDateTime.parse(dateTimeStr, getDateTimeFormatter())

        return if (exerciseId != null && runVO2Max != null && runVO2MaxError != null &&
            filteredRunVO2Max != null && filteredRunVO2MaxError != null) {
            RunVO2Max(
                exerciseId = exerciseId,
                runVO2Max = runVO2Max,
                runVO2MaxError = runVO2MaxError,
                filteredRunVO2Max = filteredRunVO2Max,
                filteredRunVO2MaxError = filteredRunVO2MaxError,
                dateTime = dateTime,
                profile = profile!!
            )
        } else null
    }
}

@Component
class ActivityGoalImporterImpl(
    repository: ActivityGoalRepository,
    entityManager: EntityManager,
    transactionManager: PlatformTransactionManager
) : CsvImporter<ActivityGoal>(repository, entityManager, transactionManager), ActivityGoalImporter {

    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    override val batchSize: Int = 10000

    override fun entityDate(entity: ActivityGoal): LocalDate = entity.createdOn.toLocalDate()

    override fun parseRow(values: List<String>, headers: List<String>): ActivityGoal? {
        if (values.size < 10) return null

        val type = values[0]
        val frequency = values[1]
        val target = values[2].toDoubleOrNull() ?: 0.0
        val result = values[3].takeIf { it != "null" }?.toDoubleOrNull()
        val status = values[4]
        val isPrimary = values[5].toBoolean()

        val startDate = values[6].takeIf { it != "null" }?.let { dateStr ->
            LocalDate.parse(dateStr, dateFormatter).atStartOfDay()
        }
        val endDate = values[7].takeIf { it != "null" }?.let { dateStr ->
            LocalDate.parse(dateStr, dateFormatter).atStartOfDay()
        }

        val createdOn = LocalDateTime.parse(values[8], dateTimeFormatter)
        val editedOn = values[9].takeIf { it != "null" }?.let {
            LocalDateTime.parse(it, dateTimeFormatter)
        }

        return ActivityGoal(
            type = type,
            frequency = frequency,
            target = target,
            result = result,
            status = status,
            isPrimary = isPrimary,
            startDate = startDate,
            endDate = endDate,
            createdOn = createdOn,
            editedOn = editedOn,
            profile = profile!!
        )
    }
}

@Component
class TimeInHeartRateZonesImporterImpl(
    repository: TimeInHeartRateZonesRepository,
    entityManager: EntityManager,
    transactionManager: PlatformTransactionManager
) : JsonImporter<TimeInHeartRateZones>(repository, entityManager, transactionManager), TimeInHeartRateZonesImporter {

    override fun entityDate(entity: TimeInHeartRateZones): LocalDate = entity.dateTime.toLocalDate()

    override fun parseToEntity(jsonItem: JsonNode): TimeInHeartRateZones? {
        val dateTimeStr = jsonItem.get("dateTime")?.asText()
        val valueNode = jsonItem.get("value")
        val valuesInZonesNode = valueNode?.get("valuesInZones")

        if (dateTimeStr != null && valuesInZonesNode != null) {
            val dateTime = LocalDateTime.parse(dateTimeStr, getDateTimeFormatter())
            val timeInHeartRateZones = TimeInHeartRateZones(dateTime, profile = profile!!)

            valuesInZonesNode.fields().forEach { (zoneName, minutesNode) ->
                val minutes = minutesNode.asDouble()
                val zoneValue = TimeInHeartRateZoneValue(zoneName, minutes, timeInHeartRateZones)
                timeInHeartRateZones.zoneValues.add(zoneValue)
            }

            return timeInHeartRateZones
        }
        return null
    }
}
