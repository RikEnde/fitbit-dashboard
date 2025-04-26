package kenny.fitbitkotlin

import kenny.fitbitkotlin.exercise.ActivityGoalImporterImpl
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class ActivityGoalImporterImplTest {

    @Autowired
    private lateinit var activityGoalImporter: ActivityGoalImporterImpl

    @Disabled("Only run manually, very long run time")
    @Test
    fun `test import returns activity goal data`() {
        // Call the import method
        val activityGoals = activityGoalImporter.import()

        // Print some sample data for manual verification
        println("Found ${activityGoals} activity goal records")
    }
}