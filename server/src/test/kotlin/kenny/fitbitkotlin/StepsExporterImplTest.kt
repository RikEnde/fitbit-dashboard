package kenny.fitbitkotlin

import kenny.fitbitkotlin.steps.Steps
import kenny.fitbitkotlin.steps.StepsExporterImpl
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.LocalDateTime

@SpringBootTest
class StepsExporterImplTest {

    @Autowired
    private lateinit var stepsExporter: StepsExporterImpl

    @Test
    fun `toRecord maps steps to HealthKit record`() {
        val steps = Steps(
            value = 1500,
            dateTime = LocalDateTime.of(2024, 6, 15, 14, 0, 0)
        )

        val record = stepsExporter.toRecord(steps)

        assertEquals("HKQuantityTypeIdentifierStepCount", record.type)
        assertEquals("count", record.unit)
        assertEquals("1500", record.value)
        assertTrue(record.startDate.contains("2024-06-15"))
    }
}
