package kenny.fitbit

import kenny.fitbit.distance.Distance
import kenny.fitbit.distance.DistanceExporterImpl
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.LocalDateTime

@SpringBootTest
class DistanceExporterImplTest {

    @Autowired
    private lateinit var distanceExporter: DistanceExporterImpl

    @Test
    fun `toRecord maps distance to HealthKit record`() {
        val distance = Distance(
            value = 2.5,
            dateTime = LocalDateTime.of(2024, 6, 15, 11, 0, 0),
            profile = TestConfig.testProfile()
        )

        val record = distanceExporter.toRecord(distance)

        assertEquals("HKQuantityTypeIdentifierDistanceWalkingRunning", record.type)
        assertEquals("km", record.unit)
        assertEquals("2.5", record.value)
        assertTrue(record.startDate.contains("2024-06-15"))
    }
}
