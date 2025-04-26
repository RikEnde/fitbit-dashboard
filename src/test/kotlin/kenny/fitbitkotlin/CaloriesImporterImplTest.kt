package kenny.fitbitkotlin

import kenny.fitbitkotlin.calories.CaloriesImporterImpl
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class CaloriesImporterImplTest {

    @Autowired
    private lateinit var caloriesImporter: CaloriesImporterImpl

    @Disabled("Only run manually, very long run time")
    @Test
    fun `test import returns calories data`() {
        // Call the import method
        val calories = caloriesImporter.import()

        // Print some sample data for manual verification
        println("Found ${calories} calories records")
    }
}
