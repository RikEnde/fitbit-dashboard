package kenny.fitbitkotlin

import com.fasterxml.jackson.databind.ObjectMapper
import kenny.fitbitkotlin.exercise.ActivityLevelRepository
import kenny.fitbitkotlin.exercise.ExerciseImporterImpl
import kenny.fitbitkotlin.exercise.ExerciseRepository
import kenny.fitbitkotlin.exercise.HeartRateZoneRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class ExerciseImporterImplTest {

    @Autowired
    private lateinit var exerciseImporter: ExerciseImporterImpl

    @Autowired
    private lateinit var exerciseRepository: ExerciseRepository

    @Autowired
    private lateinit var heartRateZoneRepository: HeartRateZoneRepository

    @Autowired
    private lateinit var activityLevelRepository: ActivityLevelRepository

    @Disabled("Only run manually, very long run time")
    @Test
    fun `test import returns exercise data`() {
        // Call the import method
        val exercises = exerciseImporter.import()

        // Print some sample data for manual verification
        println("Found ${exercises} exercise records")
    }

    @Test
    fun `test parseAndSaveEntity with heart rate zones and activity levels`() {
        // Create a sample JSON with heart rate zones and activity levels
        val jsonStr = """
        {
          "logId" : 33530682836,
          "activityName" : "Run",
          "activityTypeId" : 90009,
          "activityLevel" : [{
            "minutes" : 0,
            "name" : "sedentary"
          },{
            "minutes" : 0,
            "name" : "lightly"
          },{
            "minutes" : 0,
            "name" : "fairly"
          },{
            "minutes" : 18,
            "name" : "very"
          }],
          "averageHeartRate" : 138,
          "calories" : 248,
          "duration" : 1024000,
          "activeDuration" : 1024000,
          "steps" : 2561,
          "logType" : "auto_detected",
          "manualValuesSpecified" : {
            "calories" : false,
            "distance" : false,
            "steps" : false
          },
          "heartRateZones" : [{
            "name" : "Out of Range",
            "min" : 30,
            "max" : 87,
            "minutes" : 0
          },{
            "name" : "Fat Burn",
            "min" : 87,
            "max" : 121,
            "minutes" : 3
          },{
            "name" : "Cardio",
            "min" : 121,
            "max" : 147,
            "minutes" : 8
          },{
            "name" : "Peak",
            "min" : 147,
            "max" : 220,
            "minutes" : 7
          }],
          "lastModified" : "08/15/20 19:48:27",
          "startTime" : "08/15/20 19:25:59",
          "originalStartTime" : "08/15/20 19:25:59",
          "originalDuration" : 1024000,
          "elevationGain" : 15.24,
          "hasGps" : false,
          "shouldFetchDetails" : false,
          "hasActiveZoneMinutes" : false
        }
        """.trimIndent()

        // Parse the JSON
        val objectMapper = ObjectMapper()
        val jsonNode = objectMapper.readTree(jsonStr)

        // Clear any existing data
        heartRateZoneRepository.deleteAll()
        activityLevelRepository.deleteAll()
        exerciseRepository.deleteAll()

        // Call the method under test
        exerciseImporter.parseAndSaveEntity(jsonNode)

        // Verify that the exercise was saved
        val exercises = exerciseRepository.findAll()
        assertEquals(1, exercises.size)
        val exercise = exercises[0]

        // Verify exercise properties
        assertEquals(33530682836, exercise.logId)
        assertEquals("Run", exercise.activityName)
        assertEquals(90009, exercise.activityTypeId)
        assertEquals(138, exercise.averageHeartRate)
        assertEquals(248, exercise.calories)
        assertEquals(1024000, exercise.duration)
        assertEquals(1024000, exercise.activeDuration)
        assertEquals(2561, exercise.steps)
        assertEquals("auto_detected", exercise.logType)
        assertEquals(15.24, exercise.elevationGain)
        assertFalse(exercise.hasGps)
        assertFalse(exercise.shouldFetchDetails)
        assertFalse(exercise.hasActiveZoneMinutes)

        // Verify heart rate zones
        val heartRateZones = exercise.heartRateZones
        assertEquals(4, heartRateZones.size)

        // Verify first heart rate zone
        val outOfRangeZone = heartRateZones.find { it.name == "Out of Range" }
        assertNotNull(outOfRangeZone)
        assertEquals(30, outOfRangeZone!!.min)
        assertEquals(87, outOfRangeZone.max)
        assertEquals(0, outOfRangeZone.minutes)

        // Verify second heart rate zone
        val fatBurnZone = heartRateZones.find { it.name == "Fat Burn" }
        assertNotNull(fatBurnZone)
        assertEquals(87, fatBurnZone!!.min)
        assertEquals(121, fatBurnZone.max)
        assertEquals(3, fatBurnZone.minutes)

        // Verify third heart rate zone
        val cardioZone = heartRateZones.find { it.name == "Cardio" }
        assertNotNull(cardioZone)
        assertEquals(121, cardioZone!!.min)
        assertEquals(147, cardioZone.max)
        assertEquals(8, cardioZone.minutes)

        // Verify fourth heart rate zone
        val peakZone = heartRateZones.find { it.name == "Peak" }
        assertNotNull(peakZone)
        assertEquals(147, peakZone!!.min)
        assertEquals(220, peakZone.max)
        assertEquals(7, peakZone.minutes)

        // Verify activity levels
        val activityLevels = exercise.activityLevels
        assertEquals(4, activityLevels.size)

        // Verify first activity level
        val sedentaryLevel = activityLevels.find { it.name == "sedentary" }
        assertNotNull(sedentaryLevel)
        assertEquals(0, sedentaryLevel!!.minutes)

        // Verify second activity level
        val lightlyLevel = activityLevels.find { it.name == "lightly" }
        assertNotNull(lightlyLevel)
        assertEquals(0, lightlyLevel!!.minutes)

        // Verify third activity level
        val fairlyLevel = activityLevels.find { it.name == "fairly" }
        assertNotNull(fairlyLevel)
        assertEquals(0, fairlyLevel!!.minutes)

        // Verify fourth activity level
        val veryLevel = activityLevels.find { it.name == "very" }
        assertNotNull(veryLevel)
        assertEquals(18, veryLevel!!.minutes)
    }
}
