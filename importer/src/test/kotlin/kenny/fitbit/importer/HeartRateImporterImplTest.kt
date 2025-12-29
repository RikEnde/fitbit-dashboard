package kenny.fitbit.importer

import com.fasterxml.jackson.databind.ObjectMapper
import kenny.fitbit.importer.heartrate.HeartRateImporterImpl
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.io.File
import java.time.LocalDateTime

@SpringBootTest
class HeartRateImporterImplTest {

    @Autowired
    private lateinit var heartRateImporter: HeartRateImporterImpl

    @Test
    fun `file pattern excludes imported files`() {
        val pattern = heartRateImporter.filePattern().toRegex()
        assertTrue(pattern.matches("heart_rate-2024-01-01.json"))
        assertFalse(pattern.matches("heart_rate-2024-01-01.json.imported"))
        assertFalse(pattern.matches("resting_heart_rate-2024-01-01.json"))
    }

    @Test
    fun `parseToEntity correctly parses nested JSON to HeartRate entity`() {
        val testDataDir = javaClass.classLoader.getResource("testdata")?.path
            ?: throw IllegalStateException("Test data directory not found")

        val jsonFile = File(testDataDir, "Physical Activity/heart_rate-2024-01-01.json")
        val objectMapper = ObjectMapper()
        val jsonNode = objectMapper.readTree(jsonFile)

        val entity = heartRateImporter.parseToEntity(jsonNode[0])
        assertNotNull(entity)
        assertEquals(72, entity!!.bpm)
        assertEquals(3, entity.confidence)
        assertEquals(LocalDateTime.of(2024, 1, 1, 8, 0, 0), entity.time)

        val entity2 = heartRateImporter.parseToEntity(jsonNode[1])
        assertEquals(75, entity2!!.bpm)
        assertEquals(2, entity2.confidence)
    }

    @Test
    fun `parseToEntity returns null for missing bpm or confidence`() {
        val objectMapper = ObjectMapper()

        val missingBpm = objectMapper.readTree("""{"dateTime": "01/01/24 00:00:00", "value": {"confidence": 3}}""")
        assertNull(heartRateImporter.parseToEntity(missingBpm))

        val missingConfidence = objectMapper.readTree("""{"dateTime": "01/01/24 00:00:00", "value": {"bpm": 72}}""")
        assertNull(heartRateImporter.parseToEntity(missingConfidence))
    }
}
