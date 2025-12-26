package kenny.fitbitkotlin

import kenny.fitbitkotlin.heartrate.HeartRate
import kenny.fitbitkotlin.heartrate.HeartRateExporterImpl
import kenny.fitbitkotlin.heartrate.HeartRateRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.io.ByteArrayOutputStream
import java.time.LocalDateTime

@SpringBootTest
class HeartRateExporterImplTest {

    @Autowired
    private lateinit var heartRateExporter: HeartRateExporterImpl

    @Autowired
    private lateinit var heartRateRepository: HeartRateRepository

    @Disabled("Only run manually, requires database with data")
    @Test
    fun `test export returns heart rate data`() {
        val from = LocalDateTime.of(2024, 1, 1, 0, 0)
        val to = LocalDateTime.of(2024, 12, 31, 23, 59)
        val output = ByteArrayOutputStream()

        val count = heartRateExporter.export(from, to, output)

        println("Exported $count heart rate records")
        println("XML size: ${output.size()} bytes")
    }

    @Test
    fun `test toRecord maps heart rate correctly`() {
        val heartRate = HeartRate(
            bpm = 72,
            confidence = 3,
            time = LocalDateTime.of(2024, 6, 15, 10, 30, 0)
        )

        val record = heartRateExporter.toRecord(heartRate)

        assertEquals("HKQuantityTypeIdentifierHeartRate", record.type)
        assertEquals("Fitbit-dump", record.sourceName)
        assertEquals("count/min", record.unit)
        assertEquals("72", record.value)
        assertTrue(record.startDate.contains("2024-06-15"))
        assertTrue(record.endDate.contains("2024-06-15"))
    }

    @Test
    fun `test healthKitType returns correct identifier`() {
        assertEquals("HKQuantityTypeIdentifierHeartRate", heartRateExporter.healthKitType())
    }

    @Test
    fun `test unit returns count per min`() {
        assertEquals("count/min", heartRateExporter.unit())
    }

    @Disabled("Requires database setup")
    @Test
    fun `test export generates valid XML structure`() {
        val heartRate = HeartRate(
            bpm = 65,
            confidence = 3,
            time = LocalDateTime.of(2024, 1, 1, 8, 0, 0)
        )
        heartRateRepository.save(heartRate)

        val from = LocalDateTime.of(2024, 1, 1, 0, 0)
        val to = LocalDateTime.of(2024, 1, 1, 23, 59)
        val output = ByteArrayOutputStream()

        val count = heartRateExporter.export(from, to, output)

        assertTrue(count >= 1)
        val xml = output.toString("UTF-8")
        assertTrue(xml.contains("<?xml version=\"1.0\""))
        assertTrue(xml.contains("<HealthData"))
        assertTrue(xml.contains("HKQuantityTypeIdentifierHeartRate"))
        assertTrue(xml.contains("count/min"))
    }
}
