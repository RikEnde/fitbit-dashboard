package kenny.fitbitkotlin

import kenny.fitbitkotlin.calories.Calories
import kenny.fitbitkotlin.calories.CaloriesExporterImpl
import kenny.fitbitkotlin.calories.CaloriesRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.io.ByteArrayOutputStream
import java.time.LocalDateTime

@SpringBootTest
class CaloriesExporterImplTest {

    @Autowired
    private lateinit var caloriesExporter: CaloriesExporterImpl

    @Autowired
    private lateinit var caloriesRepository: CaloriesRepository

    @Disabled("Only run manually, requires database with data")
    @Test
    fun `test export returns calories data`() {
        val from = LocalDateTime.of(2024, 1, 1, 0, 0)
        val to = LocalDateTime.of(2024, 12, 31, 23, 59)
        val output = ByteArrayOutputStream()

        val count = caloriesExporter.export(from, to, output)

        println("Exported $count calories records")
        println("XML size: ${output.size()} bytes")
    }

    @Test
    fun `test toRecord maps calories correctly`() {
        val calories = Calories(
            value = 125.5,
            dateTime = LocalDateTime.of(2024, 6, 15, 12, 0, 0)
        )

        val record = caloriesExporter.toRecord(calories)

        assertEquals("HKQuantityTypeIdentifierActiveEnergyBurned", record.type)
        assertEquals("Fitbit-dump", record.sourceName)
        assertEquals("kcal", record.unit)
        assertEquals("125.5", record.value)
        assertTrue(record.startDate.contains("2024-06-15"))
        assertTrue(record.endDate.contains("2024-06-15"))
    }

    @Test
    fun `test healthKitType returns correct identifier`() {
        assertEquals("HKQuantityTypeIdentifierActiveEnergyBurned", caloriesExporter.healthKitType())
    }

    @Test
    fun `test unit returns kcal`() {
        assertEquals("kcal", caloriesExporter.unit())
    }

    @Disabled("Requires database setup")
    @Test
    fun `test export generates valid XML structure`() {
        val calories = Calories(
            value = 50.0,
            dateTime = LocalDateTime.of(2024, 1, 1, 10, 0, 0)
        )
        caloriesRepository.save(calories)

        val from = LocalDateTime.of(2024, 1, 1, 0, 0)
        val to = LocalDateTime.of(2024, 1, 1, 23, 59)
        val output = ByteArrayOutputStream()

        val count = caloriesExporter.export(from, to, output)

        assertTrue(count >= 1)
        val xml = output.toString("UTF-8")
        assertTrue(xml.contains("<?xml version=\"1.0\""))
        assertTrue(xml.contains("<HealthData"))
        assertTrue(xml.contains("HKQuantityTypeIdentifierActiveEnergyBurned"))
        assertTrue(xml.contains("kcal"))
    }
}
