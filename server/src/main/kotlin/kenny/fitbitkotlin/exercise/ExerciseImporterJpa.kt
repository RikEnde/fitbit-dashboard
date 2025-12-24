package kenny.fitbitkotlin.exercise

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import kenny.fitbitkotlin.heartrate.TimeInHeartRateZoneValue
import kenny.fitbitkotlin.heartrate.TimeInHeartRateZones
import kotlinx.coroutines.*
import org.springframework.stereotype.Component
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Component
class ExerciseImporterImpl(
    val repository: ExerciseRepository,
    val heartRateZoneRepository: HeartRateZoneRepository,
    val activityLevelRepository: ActivityLevelRepository
) : ExerciseImporter {

    override fun parseAndSaveEntity(jsonItem: JsonNode) {
        val logId = jsonItem.get("logId")?.asLong() ?: return
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
            hasActiveZoneMinutes = hasActiveZoneMinutes
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

        repository.save(exercise)
    }
}

@Component
class ActivityMinutesImporterImpl(val repository: ActivityMinutesRepository): ActivityMinutesImporter {
    // Track the current file being processed
    private val currentFile = ThreadLocal<String>()

    override suspend fun importFile(index: Int, size: Int, file: File, objectMapper: ObjectMapper) {
        // Store the current file name in the ThreadLocal
        currentFile.set(file.name)
        try {
            // Call the parent method to process the file
            super.importFile(index, size, file, objectMapper)
        } finally {
            // Clear the ThreadLocal
            currentFile.remove()
        }
    }

    override fun parseAndSaveEntity(jsonItem: JsonNode) {
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

            // Create and save the ActivityMinutes entity
            val activityMinutes = ActivityMinutes(
                dateTime = dateTime,
                value = value,
                intensity = intensity
            )
            repository.save(activityMinutes)
        }
    }
}

@Component
class ActiveZoneMinutesImporterImpl(val repository: ActivityMinutesRepository): ActiveZoneMinutesImporter {

    // Define a formatter for the ISO date-time format in the CSV
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")

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
                    if (parts.size == 3) {
                        val dateTimeStr = parts[0]
                        val heartZoneId = parts[1]
                        val totalMinutes = parts[2].toIntOrNull() ?: 0

                        // Parse the date time using the formatter
                        val dateTime = LocalDateTime.parse(dateTimeStr, dateTimeFormatter)

                        // Map heart zone to intensity
                        val intensity = when (heartZoneId) {
                            "FAT_BURN" -> "moderate"
                            "CARDIO" -> "active"
                            "PEAK" -> "active"
                            else -> "light" // Default to light for unknown zones
                        }

                        // Create and save the ActivityMinutes entity
                        val activityMinutes = ActivityMinutes(
                            dateTime = dateTime,
                            value = totalMinutes,
                            intensity = intensity
                        )
                        repository.save(activityMinutes)
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
class DemographicVO2MaxImporterImpl(val repository: DemographicVO2MaxRepository): DemographicVO2MaxImporter {

    override fun parseAndSaveEntity(jsonItem: JsonNode) {
        try {
            val valueNode = jsonItem.get("value")
            if (valueNode == null) {
                println("Warning: Missing 'value' node in demographic VO2 max data")
                return
            }

            val demographicVO2Max = valueNode.get("demographicVO2Max")?.asDouble()
            val demographicVO2MaxError = valueNode.get("demographicVO2MaxError")?.asDouble()
            val filteredDemographicVO2Max = valueNode.get("filteredDemographicVO2Max")?.asDouble()
            val filteredDemographicVO2MaxError = valueNode.get("filteredDemographicVO2MaxError")?.asDouble()
            val dateTimeStr = jsonItem.get("dateTime")?.asText()

            if (dateTimeStr == null) {
                println("Warning: Missing 'dateTime' in demographic VO2 max data")
                return
            }

            try {
                val dateTime: LocalDateTime = LocalDateTime.parse(dateTimeStr, getDateTimeFormatter())

                if (demographicVO2Max != null && demographicVO2MaxError != null &&
                    filteredDemographicVO2Max != null && filteredDemographicVO2MaxError != null) {
                    val vo2Max = DemographicVO2Max(
                        demographicVO2Max = demographicVO2Max,
                        demographicVO2MaxError = demographicVO2MaxError,
                        filteredDemographicVO2Max = filteredDemographicVO2Max,
                        filteredDemographicVO2MaxError = filteredDemographicVO2MaxError,
                        dateTime = dateTime
                    )
                    repository.save(vo2Max)
                    println("Successfully imported demographic VO2 max data for $dateTime")
                } else {
                    println("Warning: Missing required fields in demographic VO2 max data")
                }
            } catch (e: Exception) {
                println("Error parsing dateTime '$dateTimeStr': ${e.message}")
            }
        } catch (e: Exception) {
            println("Error processing demographic VO2 max data: ${e.message}")
        }
    }
}

@Component
class RunVO2MaxImporterImpl(val repository: RunVO2MaxRepository): RunVO2MaxImporter {

    override fun parseAndSaveEntity(jsonItem: JsonNode) {
        try {
            val valueNode = jsonItem.get("value")
            if (valueNode == null) {
                println("Warning: Missing 'value' node in run VO2 max data")
                return
            }

            val exerciseId = valueNode.get("exerciseId")?.asLong()
            val runVO2Max = valueNode.get("runVO2Max")?.asDouble()
            val runVO2MaxError = valueNode.get("runVO2MaxError")?.asDouble()
            val filteredRunVO2Max = valueNode.get("filteredRunVO2Max")?.asDouble()
            val filteredRunVO2MaxError = valueNode.get("filteredRunVO2MaxError")?.asDouble()
            val dateTimeStr = jsonItem.get("dateTime")?.asText()

            if (dateTimeStr == null) {
                println("Warning: Missing 'dateTime' in run VO2 max data")
                return
            }

            try {
                val dateTime: LocalDateTime = LocalDateTime.parse(dateTimeStr, getDateTimeFormatter())

                if (exerciseId != null && runVO2Max != null && runVO2MaxError != null &&
                    filteredRunVO2Max != null && filteredRunVO2MaxError != null) {
                    val entity = RunVO2Max(
                        exerciseId = exerciseId,
                        runVO2Max = runVO2Max,
                        runVO2MaxError = runVO2MaxError,
                        filteredRunVO2Max = filteredRunVO2Max,
                        filteredRunVO2MaxError = filteredRunVO2MaxError,
                        dateTime = dateTime
                    )
                    repository.save(entity)
                    println("Successfully imported run VO2 max data for $dateTime (exerciseId: $exerciseId)")
                } else {
                    println("Warning: Missing required fields in run VO2 max data")
                }
            } catch (e: Exception) {
                println("Error parsing dateTime '$dateTimeStr': ${e.message}")
            }
        } catch (e: Exception) {
            println("Error processing run VO2 max data: ${e.message}")
        }
    }
}

@Component
class ActivityGoalImporterImpl(val repository: ActivityGoalRepository): ActivityGoalImporter {

    // Define a formatter for the ISO date-time format in the CSV
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

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
                    if (parts.size >= 10) {
                        val type = parts[0]
                        val frequency = parts[1]
                        val target = parts[2].toDoubleOrNull() ?: 0.0
                        val result = parts[3].takeIf { it != "null" }?.toDoubleOrNull()
                        val status = parts[4]
                        val isPrimary = parts[5].toBoolean()

                        // Parse dates, handling null values
                        val startDate = parts[6].takeIf { it != "null" }?.let { dateStr ->
                            LocalDate.parse(dateStr, dateFormatter).atStartOfDay()
                        }
                        val endDate = parts[7].takeIf { it != "null" }?.let { dateStr ->
                            LocalDate.parse(dateStr, dateFormatter).atStartOfDay()
                        }

                        // Parse timestamps
                        val createdOn = LocalDateTime.parse(parts[8], dateTimeFormatter)
                        val editedOn = parts[9].takeIf { it != "null" }?.let {
                            LocalDateTime.parse(it, dateTimeFormatter)
                        }

                        // Create and save the ActivityGoal entity
                        val activityGoal = ActivityGoal(
                            type = type,
                            frequency = frequency,
                            target = target,
                            result = result,
                            status = status,
                            isPrimary = isPrimary,
                            startDate = startDate,
                            endDate = endDate,
                            createdOn = createdOn,
                            editedOn = editedOn
                        )
                        repository.save(activityGoal)
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
class TimeInHeartRateZonesImporterImpl(val repository: TimeInHeartRateZonesRepository): TimeInHeartRateZonesImporter {

    override fun getDateTimeFormatter(): DateTimeFormatter = DateTimeFormatter.ofPattern("MM/dd/yy HH:mm:ss")

    override fun parseAndSaveEntity(jsonItem: JsonNode) {
        val dateTimeStr = jsonItem.get("dateTime")?.asText()
        val valueNode = jsonItem.get("value")
        val valuesInZonesNode = valueNode?.get("valuesInZones")

        if (dateTimeStr != null && valuesInZonesNode != null) {
            val dateTime = LocalDateTime.parse(dateTimeStr, getDateTimeFormatter())
            val timeInHeartRateZones = TimeInHeartRateZones(dateTime)

            // Save the entity first to get an ID
            repository.save(timeInHeartRateZones)

            // Process each zone value
            valuesInZonesNode.fields().forEach { (zoneName, minutesNode) ->
                val minutes = minutesNode.asDouble()
                val zoneValue = TimeInHeartRateZoneValue(zoneName, minutes, timeInHeartRateZones)
                timeInHeartRateZones.zoneValues.add(zoneValue)
            }

            // Save again with the zone values
            repository.save(timeInHeartRateZones)
        }
    }
}