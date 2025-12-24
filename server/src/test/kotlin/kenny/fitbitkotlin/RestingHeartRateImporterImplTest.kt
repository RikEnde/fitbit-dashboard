package kenny.fitbitkotlin

import kenny.fitbitkotlin.heartrate.RestingHeartRateImporterImpl
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Disabled
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@SpringBootTest
class RestingHeartRateImporterImplTest {

    @Autowired
    private lateinit var restingHeartRateImporter: RestingHeartRateImporterImpl

    @Disabled("Only run manually, very long run time")
    @Test
    fun `test import returns resting heart rate data`() {
        // Call the import method
        val restingHeartRates = restingHeartRateImporter.import()

        // Print some sample data for manual verification
        println("Found ${restingHeartRates} resting heart rate records")
    }
}
