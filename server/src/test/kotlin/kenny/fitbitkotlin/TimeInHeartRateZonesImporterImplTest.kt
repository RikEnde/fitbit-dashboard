package kenny.fitbitkotlin

import kenny.fitbitkotlin.exercise.TimeInHeartRateZonesImporterImpl
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class TimeInHeartRateZonesImporterImplTest {

    @Autowired
    private lateinit var timeInHeartRateZonesImporter: TimeInHeartRateZonesImporterImpl

    @Disabled("Only run manually, very long run time")
    @Test
    fun `test import returns time in heart rate zones data`() {
        // Call the import method
        val records = timeInHeartRateZonesImporter.import()

        // Print some sample data for manual verification
        println("Found ${records} time in heart rate zones records")
    }
}
