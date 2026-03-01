package kenny.fitbit

import com.fasterxml.jackson.databind.ObjectMapper
import kenny.fitbit.sleep.SleepImporterImpl
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.io.File
import java.time.LocalDateTime

@SpringBootTest
class SleepImporterImplTest {

    @Autowired
    private lateinit var sleepImporter: SleepImporterImpl

    @BeforeEach
    fun setUp() {
        sleepImporter.profile = testProfile()
        sleepImporter.beforeImport()
    }

    @Test
    fun `file pattern excludes imported files`() {
        val pattern = sleepImporter.filePattern().toRegex()
        assertTrue(pattern.matches("sleep-2024-01-01.json"))
        assertFalse(pattern.matches("sleep-2024-01-01.json.imported"))
        assertFalse(pattern.matches("sleep_score.csv"))
    }

    @Test
    fun `parseToEntity correctly parses JSON to Sleep entity`() {
        val testDataDir = javaClass.classLoader.getResource("testdata")?.path
            ?: throw IllegalStateException("Test data directory not found")

        val jsonFile = File(testDataDir, "Sleep/sleep-2024-01-01.json")
        val objectMapper = ObjectMapper()
        val jsonNode = objectMapper.readTree(jsonFile)

        val entity = sleepImporter.parseToEntity(jsonNode[0])
        assertNotNull(entity)
        assertEquals(99999999L, entity!!.logId)
        assertEquals(LocalDateTime.of(2024, 1, 1, 0, 0, 0), entity.dateOfSleep)
        assertEquals(LocalDateTime.of(2023, 12, 31, 23, 0, 0), entity.startTime)
        assertEquals(LocalDateTime.of(2024, 1, 1, 7, 0, 0), entity.endTime)
        assertEquals(28800000L, entity.duration)
        assertEquals(420, entity.minutesAsleep)
        assertEquals(50, entity.minutesAwake)
        assertEquals(88, entity.efficiency)
        assertEquals("stages", entity.type)
        assertTrue(entity.mainSleep)
    }

    @Test
    fun `parseToEntity parses level summaries`() {
        val testDataDir = javaClass.classLoader.getResource("testdata")?.path
            ?: throw IllegalStateException("Test data directory not found")

        val jsonFile = File(testDataDir, "Sleep/sleep-2024-01-01.json")
        val objectMapper = ObjectMapper()
        val jsonNode = objectMapper.readTree(jsonFile)

        val entity = sleepImporter.parseToEntity(jsonNode[0])!!
        assertEquals(4, entity.levelSummaries.size)

        val deepSummary = entity.levelSummaries.find { it.level == "deep" }
        assertNotNull(deepSummary)
        assertEquals(3, deepSummary!!.count)
        assertEquals(60, deepSummary.minutes)
        assertEquals(55, deepSummary.thirtyDayAvgMinutes)
    }

    @Test
    fun `parseToEntity parses level data`() {
        val testDataDir = javaClass.classLoader.getResource("testdata")?.path
            ?: throw IllegalStateException("Test data directory not found")

        val jsonFile = File(testDataDir, "Sleep/sleep-2024-01-01.json")
        val objectMapper = ObjectMapper()
        val jsonNode = objectMapper.readTree(jsonFile)

        val entity = sleepImporter.parseToEntity(jsonNode[0])!!
        assertEquals(3, entity.levelData.size)

        val firstLevel = entity.levelData[0]
        assertEquals("light", firstLevel.level)
        assertEquals(600, firstLevel.seconds)
        assertEquals(LocalDateTime.of(2023, 12, 31, 23, 0, 0), firstLevel.dateTime)
    }

    @Test
    fun `parseToEntity parses short data`() {
        val testDataDir = javaClass.classLoader.getResource("testdata")?.path
            ?: throw IllegalStateException("Test data directory not found")

        val jsonFile = File(testDataDir, "Sleep/sleep-2024-01-01.json")
        val objectMapper = ObjectMapper()
        val jsonNode = objectMapper.readTree(jsonFile)

        val entity = sleepImporter.parseToEntity(jsonNode[0])!!
        assertEquals(1, entity.levelShortData.size)

        val shortData = entity.levelShortData[0]
        assertEquals("wake", shortData.level)
        assertEquals(60, shortData.seconds)
    }

    @Test
    fun `parseToEntity returns null for missing logId`() {
        val objectMapper = ObjectMapper()
        val jsonNode = objectMapper.readTree("""{"dateOfSleep": "2024-01-01"}""")

        val entity = sleepImporter.parseToEntity(jsonNode)
        assertNull(entity)
    }
}
