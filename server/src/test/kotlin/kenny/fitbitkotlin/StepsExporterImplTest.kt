package kenny.fitbitkotlin

import kenny.fitbitkotlin.steps.Steps
import kenny.fitbitkotlin.steps.StepsExporterImpl
import kenny.fitbitkotlin.steps.StepsRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.io.ByteArrayOutputStream
import java.time.LocalDateTime

@SpringBootTest
class StepsExporterImplTest {

    @Autowired
    private lateinit var stepsExporter: StepsExporterImpl

    @Autowired
    private lateinit var stepsRepository: StepsRepository

    @Disabled("Only run manually, requires database with data")
    @Test
    fun `test export returns steps data`() {
        val from = LocalDateTime.of(2024, 1, 1, 0, 0)
        val to = LocalDateTime.of(2024, 12, 31, 23, 59)
        val output = ByteArrayOutputStream()

        val count = stepsExporter.export(from, to, output)

        println("Exported $count steps records")
        println("XML size: ${output.size()} bytes")
    }

    @Test
    fun `test toRecord maps steps correctly`() {
        val steps = Steps(
            value = 1500,
            dateTime = LocalDateTime.of(2024, 6, 15, 14, 0, 0)
        )

        val record = stepsExporter.toRecord(steps)

        assertEquals("HKQuantityTypeIdentifierStepCount", record.type)
        assertEquals("Fitbit-dump", record.sourceName)
        assertEquals("count", record.unit)
        assertEquals("1500", record.value)
        assertTrue(record.startDate.contains("2024-06-15"))
        assertTrue(record.endDate.contains("2024-06-15"))
    }

    @Test
    fun `test healthKitType returns correct identifier`() {
        assertEquals("HKQuantityTypeIdentifierStepCount", stepsExporter.healthKitType())
    }

    @Test
    fun `test unit returns count`() {
        assertEquals("count", stepsExporter.unit())
    }

    @Disabled("Requires database setup")
    @Test
    fun `test export generates valid XML structure`() {
        val steps = Steps(
            value = 250,
            dateTime = LocalDateTime.of(2024, 1, 1, 9, 0, 0)
        )
        stepsRepository.save(steps)

        val from = LocalDateTime.of(2024, 1, 1, 0, 0)
        val to = LocalDateTime.of(2024, 1, 1, 23, 59)
        val output = ByteArrayOutputStream()

        val count = stepsExporter.export(from, to, output)

        assertTrue(count >= 1)
        val xml = output.toString("UTF-8")
        assertTrue(xml.contains("<?xml version=\"1.0\""))
        assertTrue(xml.contains("<HealthData"))
        assertTrue(xml.contains("HKQuantityTypeIdentifierStepCount"))
        assertTrue(xml.contains("count"))
    }
}
