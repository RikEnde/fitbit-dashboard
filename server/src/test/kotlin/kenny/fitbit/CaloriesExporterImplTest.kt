package kenny.fitbit

import kenny.fitbit.calories.Calories
import kenny.fitbit.calories.CaloriesExporterImpl
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.LocalDateTime

@SpringBootTest
class CaloriesExporterImplTest {

    @Autowired
    private lateinit var caloriesExporter: CaloriesExporterImpl

    @Test
    fun `toRecord maps calories to HealthKit record`() {
        val calories = Calories(
            value = 125.5,
            dateTime = LocalDateTime.of(2024, 6, 15, 12, 0, 0),
            profile = TestConfig.testProfile()
        )

        val record = caloriesExporter.toRecord(calories)

        assertEquals("HKQuantityTypeIdentifierActiveEnergyBurned", record.type)
        assertEquals("kcal", record.unit)
        assertEquals("125.5", record.value)
        assertTrue(record.startDate.contains("2024-06-15"))
    }
}
