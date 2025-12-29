package kenny.fitbitkotlin.importer

import com.fasterxml.jackson.databind.ObjectMapper
import kenny.fitbitkotlin.importer.steps.StepsImporterImpl
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.io.File
import java.time.LocalDateTime

@SpringBootTest
class StepsImporterImplTest {

    @Autowired
    private lateinit var stepsImporter: StepsImporterImpl

    @Test
    fun `file pattern excludes imported files`() {
        val pattern = stepsImporter.filePattern().toRegex()
        assertTrue(pattern.matches("steps-2024-01-01.json"))
        assertFalse(pattern.matches("steps-2024-01-01.json.imported"))
        assertFalse(pattern.matches("distance-2024-01-01.json"))
    }

    @Test
    fun `parseToEntity correctly parses JSON to Steps entity`() {
        val testDataDir = javaClass.classLoader.getResource("testdata")?.path
            ?: throw IllegalStateException("Test data directory not found")

        val jsonFile = File(testDataDir, "Physical Activity/steps-2024-01-01.json")
        val objectMapper = ObjectMapper()
        val jsonNode = objectMapper.readTree(jsonFile)

        val entity = stepsImporter.parseToEntity(jsonNode[0])
        assertNotNull(entity)
        assertEquals(150, entity!!.value)
        assertEquals(LocalDateTime.of(2024, 1, 1, 9, 0, 0), entity.dateTime)

        val entity2 = stepsImporter.parseToEntity(jsonNode[1])
        assertEquals(200, entity2!!.value)
    }

    @Test
    fun `parseToEntity returns null for missing value`() {
        val objectMapper = ObjectMapper()
        val jsonNode = objectMapper.readTree("""{"dateTime": "01/01/24 00:00:00"}""")

        val entity = stepsImporter.parseToEntity(jsonNode)
        assertNull(entity)
    }
}
