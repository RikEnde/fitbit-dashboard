package kenny.fitbitkotlin

import kenny.fitbitkotlin.distance.Distance
import kenny.fitbitkotlin.distance.DistanceExporterImpl
import kenny.fitbitkotlin.distance.DistanceRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.io.ByteArrayOutputStream
import java.time.LocalDateTime

@SpringBootTest
class DistanceExporterImplTest {

    @Autowired
    private lateinit var distanceExporter: DistanceExporterImpl

    @Autowired
    private lateinit var distanceRepository: DistanceRepository

    @Disabled("Only run manually, requires database with data")
    @Test
    fun `test export returns distance data`() {
        val from = LocalDateTime.of(2024, 1, 1, 0, 0)
        val to = LocalDateTime.of(2024, 12, 31, 23, 59)
        val output = ByteArrayOutputStream()

        val count = distanceExporter.export(from, to, output)

        println("Exported $count distance records")
        println("XML size: ${output.size()} bytes")
    }

    @Test
    fun `test toRecord maps distance correctly`() {
        val distance = Distance(
            value = 2.5,
            dateTime = LocalDateTime.of(2024, 6, 15, 11, 0, 0)
        )

        val record = distanceExporter.toRecord(distance)

        assertEquals("HKQuantityTypeIdentifierDistanceWalkingRunning", record.type)
        assertEquals("Fitbit-dump", record.sourceName)
        assertEquals("km", record.unit)
        assertEquals("2.5", record.value)
        assertTrue(record.startDate.contains("2024-06-15"))
        assertTrue(record.endDate.contains("2024-06-15"))
    }

    @Test
    fun `test healthKitType returns correct identifier`() {
        assertEquals("HKQuantityTypeIdentifierDistanceWalkingRunning", distanceExporter.healthKitType())
    }

    @Test
    fun `test unit returns km`() {
        assertEquals("km", distanceExporter.unit())
    }

    @Disabled("Requires database setup")
    @Test
    fun `test export generates valid XML structure`() {
        val distance = Distance(
            value = 0.75,
            dateTime = LocalDateTime.of(2024, 1, 1, 11, 0, 0)
        )
        distanceRepository.save(distance)

        val from = LocalDateTime.of(2024, 1, 1, 0, 0)
        val to = LocalDateTime.of(2024, 1, 1, 23, 59)
        val output = ByteArrayOutputStream()

        val count = distanceExporter.export(from, to, output)

        assertTrue(count >= 1)
        val xml = output.toString("UTF-8")
        assertTrue(xml.contains("<?xml version=\"1.0\""))
        assertTrue(xml.contains("<HealthData"))
        assertTrue(xml.contains("HKQuantityTypeIdentifierDistanceWalkingRunning"))
        assertTrue(xml.contains("km"))
    }
}
