package kenny.fitbitkotlin

import kenny.fitbitkotlin.distance.DistanceImporterImpl
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class DistanceImporterImplTest {

    @Autowired
    private lateinit var distanceImporter: DistanceImporterImpl

    @Disabled("Only run manually, very long run time")
    @Test
    fun `test import returns distance data`() {
        // Call the import method
        val distances = distanceImporter.import()

        // Print some sample data for manual verification
        println("Found ${distances} distance records")
    }
}
