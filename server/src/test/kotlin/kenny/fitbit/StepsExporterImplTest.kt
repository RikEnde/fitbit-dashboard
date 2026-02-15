package kenny.fitbit

import kenny.fitbit.steps.Steps
import kenny.fitbit.steps.StepsExporterImpl
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import java.time.LocalDateTime

@SpringBootTest
@Import(TestConfig::class)
class StepsExporterImplTest {

    @Autowired
    private lateinit var stepsExporter: StepsExporterImpl

    @Test
    fun `toRecord maps steps to HealthKit record`() {
        val steps = Steps(
            value = 1500,
            dateTime = LocalDateTime.of(2024, 6, 15, 14, 0, 0),
            profile = TestConfig.testProfile()
        )

        val record = stepsExporter.toRecord(steps)

        assertEquals("HKQuantityTypeIdentifierStepCount", record.type)
        assertEquals("count", record.unit)
        assertEquals("1500", record.value)
        assertTrue(record.startDate.contains("2024-06-15"))
    }
}
