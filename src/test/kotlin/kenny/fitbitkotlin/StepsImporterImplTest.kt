package kenny.fitbitkotlin

import kenny.fitbitkotlin.steps.StepsImporterImpl
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class StepsImporterImplTest {

    @Autowired
    private lateinit var stepsImporter: StepsImporterImpl

    @Disabled("Only run manually, very long run time")
    @Test
    fun `test import returns steps data`() {
        // Call the import method
        val steps = stepsImporter.import()

        // Print some sample data for manual verification
        println("Found ${steps} steps records")
    }
}
