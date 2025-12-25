package kenny.fitbitkotlin

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.persistence.EntityManager
import kenny.fitbitkotlin.exercise.RunVO2Max
import kenny.fitbitkotlin.exercise.RunVO2MaxImporterImpl
import kenny.fitbitkotlin.exercise.RunVO2MaxRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.LocalDateTime

@SpringBootTest
class RunVO2MaxImporterImplTest {

    @Autowired
    private lateinit var runVO2MaxImporter: RunVO2MaxImporterImpl

    @Disabled("Only run manually, very long run time")
    @Test
    fun `test import returns run vo2 max data`() {
        // Call the import method
        val runVO2MaxCount = runVO2MaxImporter.import()

        // Print some sample data for manual verification
        println("Found ${runVO2MaxCount} run VO2 max records")
    }
}

@ExtendWith(MockitoExtension::class)
class RunVO2MaxImporterUnitTest {

    @Mock
    private lateinit var repository: RunVO2MaxRepository

    @Mock
    private lateinit var entityManager: EntityManager

    @InjectMocks
    private lateinit var importer: RunVO2MaxImporterImpl

    @Captor
    private lateinit var vo2MaxCaptor: ArgumentCaptor<RunVO2Max>

    @Test
    fun `test parseAndSaveEntity with valid data`() {
        // Create a mock JSON node with valid data
        val objectMapper = ObjectMapper()
        val rootNode = objectMapper.createObjectNode()
        val valueNode = objectMapper.createObjectNode()

        valueNode.put("exerciseId", 44261899226)
        valueNode.put("runVO2Max", 52.64144)
        valueNode.put("runVO2MaxError", 3.92498)
        valueNode.put("filteredRunVO2Max", 53.52355)
        valueNode.put("filteredRunVO2MaxError", 0.96033)
        rootNode.set<JsonNode>("value", valueNode)
        rootNode.put("dateTime", "11/18/21 16:42:56")

        // Call the method under test
        val result = importer.parseToEntity(rootNode)

        // Verify the result is not null
        assertNotNull(result)

        // Verify the entity has the correct values
        assertEquals(44261899226, result!!.exerciseId)
        assertEquals(52.64144, result.runVO2Max)
        assertEquals(3.92498, result.runVO2MaxError)
        assertEquals(53.52355, result.filteredRunVO2Max)
        assertEquals(0.96033, result.filteredRunVO2MaxError)

        // Verify the date was parsed correctly
        val expectedDateTime = LocalDateTime.of(2021, 11, 18, 16, 42, 56)
        assertEquals(expectedDateTime, result.dateTime)
    }

    @Test
    fun `test parseAndSaveEntity with missing value node`() {
        // Create a mock JSON node with missing value node
        val objectMapper = ObjectMapper()
        val rootNode = objectMapper.createObjectNode()
        rootNode.put("dateTime", "11/18/21 16:42:56")

        // Call the method under test
        val result = importer.parseToEntity(rootNode)

        // Verify that the result is null
        assertNull(result)
    }

    @Test
    fun `test parseAndSaveEntity with missing dateTime`() {
        // Create a mock JSON node with missing dateTime
        val objectMapper = ObjectMapper()
        val rootNode = objectMapper.createObjectNode()
        val valueNode = objectMapper.createObjectNode()

        valueNode.put("exerciseId", 44261899226)
        valueNode.put("runVO2Max", 52.64144)
        valueNode.put("runVO2MaxError", 3.92498)
        valueNode.put("filteredRunVO2Max", 53.52355)
        valueNode.put("filteredRunVO2MaxError", 0.96033)
        rootNode.set<JsonNode>("value", valueNode)

        // Call the method under test
        val result = importer.parseToEntity(rootNode)

        // Verify that the result is null
        assertNull(result)
    }

    @Test
    fun `test parseAndSaveEntity with invalid dateTime format`() {
        // Create a mock JSON node with invalid dateTime format
        val objectMapper = ObjectMapper()
        val rootNode = objectMapper.createObjectNode()
        val valueNode = objectMapper.createObjectNode()

        valueNode.put("exerciseId", 44261899226)
        valueNode.put("runVO2Max", 52.64144)
        valueNode.put("runVO2MaxError", 3.92498)
        valueNode.put("filteredRunVO2Max", 53.52355)
        valueNode.put("filteredRunVO2MaxError", 0.96033)
        rootNode.set<JsonNode>("value", valueNode)
        rootNode.put("dateTime", "2021-11-18T16:42:56") // Wrong format

        // Call the method under test
        val result = importer.parseToEntity(rootNode)

        // Verify that the result is null
        assertNull(result)
    }

    @Test
    fun `test parseAndSaveEntity with missing required fields`() {
        // Create a mock JSON node with missing required fields
        val objectMapper = ObjectMapper()
        val rootNode = objectMapper.createObjectNode()
        val valueNode = objectMapper.createObjectNode()

        valueNode.put("exerciseId", 44261899226)
        valueNode.put("runVO2Max", 52.64144)
        // Missing runVO2MaxError
        valueNode.put("filteredRunVO2Max", 53.52355)
        valueNode.put("filteredRunVO2MaxError", 0.96033)
        rootNode.set<JsonNode>("value", valueNode)
        rootNode.put("dateTime", "11/18/21 16:42:56")

        // Call the method under test
        val result = importer.parseToEntity(rootNode)

        // Verify that the result is null
        assertNull(result)
    }
}