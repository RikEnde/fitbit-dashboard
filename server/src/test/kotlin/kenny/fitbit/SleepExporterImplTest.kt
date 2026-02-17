package kenny.fitbit

import kenny.fitbit.sleep.Sleep
import kenny.fitbit.sleep.SleepExporterImpl
import kenny.fitbit.sleep.SleepLevelData
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.LocalDateTime

@SpringBootTest
class SleepExporterImplTest {

    @Autowired
    private lateinit var sleepExporter: SleepExporterImpl

    private val testProfile = TestConfig.testProfile()

    @Test
    fun `mapFitbitLevelToAppleHealth maps sleep stages correctly`() {
        assertEquals(2, sleepExporter.mapFitbitLevelToAppleHealth("wake"))
        assertEquals(2, sleepExporter.mapFitbitLevelToAppleHealth("awake"))
        assertEquals(3, sleepExporter.mapFitbitLevelToAppleHealth("light"))
        assertEquals(4, sleepExporter.mapFitbitLevelToAppleHealth("deep"))
        assertEquals(5, sleepExporter.mapFitbitLevelToAppleHealth("rem"))
        assertEquals(1, sleepExporter.mapFitbitLevelToAppleHealth("asleep"))
        assertEquals(0, sleepExporter.mapFitbitLevelToAppleHealth("unknown"))
    }

    @Test
    fun `toRecord maps sleep level data to HealthKit record`() {
        val sleep = Sleep(
            logId = 12345L,
            dateOfSleep = LocalDateTime.of(2024, 6, 15, 0, 0),
            startTime = LocalDateTime.of(2024, 6, 14, 22, 0),
            endTime = LocalDateTime.of(2024, 6, 15, 6, 0),
            duration = 28800000,
            minutesToFallAsleep = 10,
            minutesAsleep = 450,
            minutesAwake = 30,
            minutesAfterWakeup = 0,
            timeInBed = 480,
            efficiency = 94,
            type = "stages",
            infoCode = 0,
            logType = "auto_detected",
            mainSleep = true,
            profile = testProfile
        )

        val sleepLevelData = SleepLevelData(
            dateTime = LocalDateTime.of(2024, 6, 14, 22, 15, 0),
            level = "deep",
            seconds = 1800,
            sleep = sleep
        )

        val record = sleepExporter.toRecord(sleepLevelData)

        assertEquals("HKCategoryTypeIdentifierSleepAnalysis", record.type)
        assertEquals("4", record.value)  // deep = 4
        assertTrue(record.startDate.contains("2024-06-14"))
    }

    @Test
    fun `toRecord calculates end time from seconds`() {
        val sleep = Sleep(
            logId = 12346L,
            dateOfSleep = LocalDateTime.of(2024, 6, 15, 0, 0),
            startTime = LocalDateTime.of(2024, 6, 14, 22, 0),
            endTime = LocalDateTime.of(2024, 6, 15, 6, 0),
            duration = 28800000,
            minutesToFallAsleep = 10,
            minutesAsleep = 450,
            minutesAwake = 30,
            minutesAfterWakeup = 0,
            timeInBed = 480,
            efficiency = 94,
            type = "stages",
            infoCode = 0,
            logType = "auto_detected",
            mainSleep = true,
            profile = testProfile
        )

        val sleepLevelData = SleepLevelData(
            dateTime = LocalDateTime.of(2024, 6, 14, 23, 0, 0),
            level = "light",
            seconds = 3600,  // 1 hour
            sleep = sleep
        )

        val record = sleepExporter.toRecord(sleepLevelData)

        assertTrue(record.startDate.contains("23:00:00"))
        assertTrue(record.endDate.contains("00:00:00"))
    }
}
