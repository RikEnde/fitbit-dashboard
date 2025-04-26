package kenny.fitbitkotlin

import com.fasterxml.jackson.databind.ObjectMapper
import kenny.fitbitkotlin.sleep.SleepImporterImpl
import kenny.fitbitkotlin.sleep.SleepRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class SleepImporterImplTest {

    @Autowired
    private lateinit var sleepImporter: SleepImporterImpl

    @Autowired
    private lateinit var sleepRepository: SleepRepository

    @Test
    fun `test import returns sleep data`() {
        // Call the import method
        val sleepRecords = sleepImporter.import()

        // Print some sample data for manual verification
        println("Found ${sleepRecords} sleep records")

        // Call import again to simulate duplicate imports
        println("Calling import again to test for duplicate key errors")
        val sleepRecords2 = sleepImporter.import()
        println("Found ${sleepRecords2} sleep records on second import")
    }

    @Test
    fun `test duplicate sleep data is handled correctly`() {
        // Create a sample sleep data JSON node
        val objectMapper = ObjectMapper()
        val sampleSleepJson = """
        {
            "logId": 123456789,
            "dateOfSleep": "2023-01-01",
            "startTime": "2023-01-01T22:00:00.000",
            "endTime": "2023-01-02T06:00:00.000",
            "duration": 28800000,
            "minutesToFallAsleep": 10,
            "minutesAsleep": 480,
            "minutesAwake": 0,
            "minutesAfterWakeup": 0,
            "timeInBed": 480,
            "efficiency": 100,
            "type": "stages",
            "infoCode": 0,
            "logType": "auto_detected",
            "mainSleep": true,
            "levels": {
                "summary": {
                    "deep": {
                        "count": 4,
                        "minutes": 120,
                        "thirtyDayAvgMinutes": 120
                    },
                    "light": {
                        "count": 10,
                        "minutes": 240,
                        "thirtyDayAvgMinutes": 240
                    },
                    "rem": {
                        "count": 5,
                        "minutes": 120,
                        "thirtyDayAvgMinutes": 120
                    }
                },
                "data": [],
                "shortData": []
            }
        }
        """
        val jsonNode = objectMapper.readTree(sampleSleepJson)

        // First call should succeed
        sleepImporter.parseAndSaveEntity(jsonNode)
        println("First save succeeded")

        // Second call should skip the import rather than throw an exception
        sleepImporter.parseAndSaveEntity(jsonNode)
        println("Second call completed without error")

        // Verify that only one record was saved
        val allSleepRecords = sleepRepository.findAll()
        val recordsWithMatchingLogId = allSleepRecords.filter { it.logId == 123456789L }
        assertEquals(1, recordsWithMatchingLogId.size, "Expected only one record with logId 123456789 but found ${recordsWithMatchingLogId.size}")
    }
}
