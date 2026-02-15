package kenny.fitbit

import kenny.fitbit.heartrate.HeartRate
import kenny.fitbit.heartrate.HeartRateExporterImpl
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import java.time.LocalDateTime

@SpringBootTest
@Import(TestConfig::class)
class HeartRateExporterImplTest {

    @Autowired
    private lateinit var heartRateExporter: HeartRateExporterImpl

    @Test
    fun `toRecord maps heart rate to HealthKit record`() {
        val heartRate = HeartRate(
            bpm = 72,
            confidence = 3,
            time = LocalDateTime.of(2024, 6, 15, 10, 30, 0),
            profile = TestConfig.testProfile()
        )

        val record = heartRateExporter.toRecord(heartRate)

        assertEquals("HKQuantityTypeIdentifierHeartRate", record.type)
        assertEquals("count/min", record.unit)
        assertEquals("72", record.value)
        assertTrue(record.startDate.contains("2024-06-15"))
    }
}
