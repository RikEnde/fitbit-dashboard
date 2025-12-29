package kenny.fitbit

import com.fasterxml.jackson.databind.ObjectMapper
import kenny.fitbit.distance.DistanceImporterImpl
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.io.File
import java.time.LocalDateTime

@SpringBootTest
class DistanceImporterImplTest {

    @Autowired
    private lateinit var distanceImporter: DistanceImporterImpl

    @Test
    fun `file pattern excludes imported files`() {
        val pattern = distanceImporter.filePattern().toRegex()
        assertTrue(pattern.matches("distance-2024-01-01.json"))
        assertFalse(pattern.matches("distance-2024-01-01.json.imported"))
        assertFalse(pattern.matches("steps-2024-01-01.json"))
    }

    @Test
    fun `parseToEntity correctly parses JSON to Distance entity`() {
        val testDataDir = javaClass.classLoader.getResource("testdata")?.path
            ?: throw IllegalStateException("Test data directory not found")

        val jsonFile = File(testDataDir, "Physical Activity/distance-2024-01-01.json")
        val objectMapper = ObjectMapper()
        val jsonNode = objectMapper.readTree(jsonFile)

        val entity = distanceImporter.parseToEntity(jsonNode[0])
        assertNotNull(entity)
        assertEquals(150.5, entity!!.value)
        assertEquals(LocalDateTime.of(2024, 1, 1, 10, 0, 0), entity.dateTime)

        val entity2 = distanceImporter.parseToEntity(jsonNode[1])
        assertEquals(200.75, entity2!!.value)
    }

    @Test
    fun `parseToEntity returns null for missing value`() {
        val objectMapper = ObjectMapper()
        val jsonNode = objectMapper.readTree("""{"dateTime": "01/01/24 00:00:00"}""")

        val entity = distanceImporter.parseToEntity(jsonNode)
        assertNull(entity)
    }
}
