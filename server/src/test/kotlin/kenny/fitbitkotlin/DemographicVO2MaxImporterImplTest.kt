package kenny.fitbitkotlin

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import kenny.fitbitkotlin.exercise.DemographicVO2Max
import kenny.fitbitkotlin.exercise.DemographicVO2MaxImporterImpl
import kenny.fitbitkotlin.exercise.DemographicVO2MaxRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.LocalDateTime

@SpringBootTest
class DemographicVO2MaxImporterImplTest {

    @Autowired
    private lateinit var demographicVO2MaxImporter: DemographicVO2MaxImporterImpl

    @Disabled("Only run manually, very long run time")
    @Test
    fun `test import returns demographic vo2 max data`() {
        // Call the import method
        val demographicVO2MaxCount = demographicVO2MaxImporter.import()

        // Print some sample data for manual verification
        println("Found ${demographicVO2MaxCount} demographic VO2 max records")
    }
}

@ExtendWith(MockitoExtension::class)
class DemographicVO2MaxImporterUnitTest {

    @Mock
    private lateinit var repository: DemographicVO2MaxRepository

    @InjectMocks
    private lateinit var importer: DemographicVO2MaxImporterImpl

    @Captor
    private lateinit var vo2MaxCaptor: ArgumentCaptor<DemographicVO2Max>

    @Test
    fun `test parseAndSaveEntity with valid data`() {
        // Create a mock JSON node with valid data
        val objectMapper = ObjectMapper()
        val rootNode = objectMapper.createObjectNode()
        val valueNode = objectMapper.createObjectNode()

        valueNode.put("demographicVO2Max", 45.5)
        valueNode.put("demographicVO2MaxError", 1.2)
        valueNode.put("filteredDemographicVO2Max", 44.8)
        valueNode.put("filteredDemographicVO2MaxError", 0.9)
        rootNode.set<JsonNode>("value", valueNode)
        rootNode.put("dateTime", "06/15/22 10:30:00")

        // Call the method under test
        importer.parseAndSaveEntity(rootNode)

        // Verify that repository.save was called once
        verify(repository, times(1)).save(vo2MaxCaptor.capture())

        // Verify the captured entity has the correct values
        val capturedVO2Max = vo2MaxCaptor.value
        assertEquals(45.5, capturedVO2Max.demographicVO2Max)
        assertEquals(1.2, capturedVO2Max.demographicVO2MaxError)
        assertEquals(44.8, capturedVO2Max.filteredDemographicVO2Max)
        assertEquals(0.9, capturedVO2Max.filteredDemographicVO2MaxError)

        // Verify the date was parsed correctly
        val expectedDateTime = LocalDateTime.of(2022, 6, 15, 10, 30, 0)
        assertEquals(expectedDateTime, capturedVO2Max.dateTime)
    }

    @Test
    fun `test parseAndSaveEntity with missing value node`() {
        // Create a mock JSON node with missing value node
        val objectMapper = ObjectMapper()
        val rootNode = objectMapper.createObjectNode()
        rootNode.put("dateTime", "06/15/22 10:30:00")

        // Call the method under test
        importer.parseAndSaveEntity(rootNode)

        // Verify that repository.save was not called
        verify(repository, never()).save(any())
    }

    @Test
    fun `test parseAndSaveEntity with missing dateTime`() {
        // Create a mock JSON node with missing dateTime
        val objectMapper = ObjectMapper()
        val rootNode = objectMapper.createObjectNode()
        val valueNode = objectMapper.createObjectNode()

        valueNode.put("demographicVO2Max", 45.5)
        valueNode.put("demographicVO2MaxError", 1.2)
        valueNode.put("filteredDemographicVO2Max", 44.8)
        valueNode.put("filteredDemographicVO2MaxError", 0.9)
        rootNode.set<JsonNode>("value", valueNode)

        // Call the method under test
        importer.parseAndSaveEntity(rootNode)

        // Verify that repository.save was not called
        verify(repository, never()).save(any())
    }

    @Test
    fun `test parseAndSaveEntity with invalid dateTime format`() {
        // Create a mock JSON node with invalid dateTime format
        val objectMapper = ObjectMapper()
        val rootNode = objectMapper.createObjectNode()
        val valueNode = objectMapper.createObjectNode()

        valueNode.put("demographicVO2Max", 45.5)
        valueNode.put("demographicVO2MaxError", 1.2)
        valueNode.put("filteredDemographicVO2Max", 44.8)
        valueNode.put("filteredDemographicVO2MaxError", 0.9)
        rootNode.set<JsonNode>("value", valueNode)
        rootNode.put("dateTime", "2022-06-15T10:30:00") // Wrong format

        // Call the method under test
        importer.parseAndSaveEntity(rootNode)

        // Verify that repository.save was not called
        verify(repository, never()).save(any())
    }

    @Test
    fun `test parseAndSaveEntity with missing required fields`() {
        // Create a mock JSON node with missing required fields
        val objectMapper = ObjectMapper()
        val rootNode = objectMapper.createObjectNode()
        val valueNode = objectMapper.createObjectNode()

        valueNode.put("demographicVO2Max", 45.5)
        // Missing demographicVO2MaxError
        valueNode.put("filteredDemographicVO2Max", 44.8)
        valueNode.put("filteredDemographicVO2MaxError", 0.9)
        rootNode.set<JsonNode>("value", valueNode)
        rootNode.put("dateTime", "06/15/22 10:30:00")

        // Call the method under test
        importer.parseAndSaveEntity(rootNode)

        // Verify that repository.save was not called
        verify(repository, never()).save(any())
    }
}
