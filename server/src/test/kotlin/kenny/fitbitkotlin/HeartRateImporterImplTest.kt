package kenny.fitbitkotlin

import kenny.fitbitkotlin.heartrate.HeartRateImporterImpl
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class HeartRateImporterImplTest {

    @Autowired
    private lateinit var heartRateImporter: HeartRateImporterImpl

    @Disabled("Only run manually, very long run time")
    @Test
    fun `test import returns heart rate data`() {
        // Call the import method
        val heartRates = heartRateImporter.import()

        // Print some sample data for manual verification
        println("Found ${heartRates} heart rate records")
    }
}
