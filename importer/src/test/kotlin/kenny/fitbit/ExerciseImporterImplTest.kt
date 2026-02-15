package kenny.fitbit

import com.fasterxml.jackson.databind.ObjectMapper
import kenny.fitbit.exercise.ExerciseImporterImpl
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.io.File
import java.time.LocalDateTime

@SpringBootTest
class ExerciseImporterImplTest {

    @Autowired
    private lateinit var exerciseImporter: ExerciseImporterImpl

    @BeforeEach
    fun setUp() {
        exerciseImporter.profile = testProfile()
    }

    @Test
    fun `file pattern matches exercise files`() {
        val pattern = exerciseImporter.filePattern().toRegex()
        assertTrue(pattern.matches("exercise-0.json"))
        assertTrue(pattern.matches("exercise-100.json"))
        assertTrue(pattern.matches("exercise-1000.json"))
        assertFalse(pattern.matches("exercise-0.json.imported"))
    }

    @Test
    fun `parseToEntity correctly parses JSON to Exercise entity with nested data`() {
        val testDataDir = javaClass.classLoader.getResource("testdata")?.path
            ?: throw IllegalStateException("Test data directory not found")

        val jsonFile = File(testDataDir, "Physical Activity/exercise-0.json")
        val objectMapper = ObjectMapper()
        val jsonNode = objectMapper.readTree(jsonFile)

        val entity = exerciseImporter.parseToEntity(jsonNode[0])
        assertNotNull(entity)
        assertEquals(12345678L, entity!!.logId)
        assertEquals("Running", entity.activityName)
        assertEquals(90009, entity.activityTypeId)
        assertEquals(145, entity.averageHeartRate)
        assertEquals(350, entity.calories)
        assertEquals(2520000L, entity.duration)
        assertEquals(4200, entity.steps)
        assertEquals(LocalDateTime.of(2024, 1, 1, 14, 0, 0), entity.startTime)
        assertTrue(entity.hasGps)
        assertTrue(entity.hasActiveZoneMinutes)
    }

    @Test
    fun `parseToEntity parses heart rate zones`() {
        val testDataDir = javaClass.classLoader.getResource("testdata")?.path
            ?: throw IllegalStateException("Test data directory not found")

        val jsonFile = File(testDataDir, "Physical Activity/exercise-0.json")
        val objectMapper = ObjectMapper()
        val jsonNode = objectMapper.readTree(jsonFile)

        val entity = exerciseImporter.parseToEntity(jsonNode[0])!!
        assertEquals(4, entity.heartRateZones.size)

        val peakZone = entity.heartRateZones.find { it.name == "Peak" }
        assertNotNull(peakZone)
        assertEquals(153, peakZone!!.min)
        assertEquals(220, peakZone.max)
        assertEquals(12, peakZone.minutes)
    }

    @Test
    fun `parseToEntity parses activity levels`() {
        val testDataDir = javaClass.classLoader.getResource("testdata")?.path
            ?: throw IllegalStateException("Test data directory not found")

        val jsonFile = File(testDataDir, "Physical Activity/exercise-0.json")
        val objectMapper = ObjectMapper()
        val jsonNode = objectMapper.readTree(jsonFile)

        val entity = exerciseImporter.parseToEntity(jsonNode[0])!!
        assertEquals(4, entity.activityLevels.size)

        val veryActive = entity.activityLevels.find { it.name == "very" }
        assertNotNull(veryActive)
        assertEquals(25, veryActive!!.minutes)
    }

    @Test
    fun `parseToEntity returns null for missing logId`() {
        val objectMapper = ObjectMapper()
        val jsonNode = objectMapper.readTree("""{"activityName": "Running"}""")

        val entity = exerciseImporter.parseToEntity(jsonNode)
        assertNull(entity)
    }
}
