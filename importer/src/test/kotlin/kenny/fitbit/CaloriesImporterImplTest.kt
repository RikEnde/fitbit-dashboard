package kenny.fitbit

import com.fasterxml.jackson.databind.ObjectMapper
import kenny.fitbit.calories.CaloriesImporterImpl
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.io.File
import java.time.LocalDateTime

@SpringBootTest
class CaloriesImporterImplTest {

    @Autowired
    private lateinit var caloriesImporter: CaloriesImporterImpl

    @BeforeEach
    fun setUp() {
        caloriesImporter.profile = testProfile()
    }

    @Test
    fun `file pattern excludes imported files`() {
        val pattern = caloriesImporter.filePattern().toRegex()
        assertTrue(pattern.matches("calories-2024-01-01.json"))
        assertFalse(pattern.matches("calories-2024-01-01.json.imported"))
        assertFalse(pattern.matches("steps-2024-01-01.json"))
    }

    @Test
    fun `parseToEntity correctly parses JSON to Calories entity`() {
        val testDataDir = javaClass.classLoader.getResource("testdata")?.path
            ?: throw IllegalStateException("Test data directory not found")

        val jsonFile = File(testDataDir, "Physical Activity/calories-2024-01-01.json")
        val objectMapper = ObjectMapper()
        val jsonNode = objectMapper.readTree(jsonFile)

        val entity = caloriesImporter.parseToEntity(jsonNode[0])
        assertNotNull(entity)
        assertEquals(1.25, entity!!.value)
        assertEquals(LocalDateTime.of(2024, 1, 1, 0, 0, 0), entity.dateTime)

        val entity2 = caloriesImporter.parseToEntity(jsonNode[4])
        assertEquals(3.75, entity2!!.value)
        assertEquals(LocalDateTime.of(2024, 1, 1, 0, 4, 0), entity2.dateTime)
    }

    @Test
    fun `parseToEntity returns null for missing value`() {
        val objectMapper = ObjectMapper()
        val jsonNode = objectMapper.readTree("""{"dateTime": "01/01/24 00:00:00"}""")

        val entity = caloriesImporter.parseToEntity(jsonNode)
        assertNull(entity)
    }
}
