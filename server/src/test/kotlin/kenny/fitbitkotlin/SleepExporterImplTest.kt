package kenny.fitbitkotlin

import kenny.fitbitkotlin.sleep.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.io.ByteArrayOutputStream
import java.time.LocalDateTime

@SpringBootTest
class SleepExporterImplTest {

    @Autowired
    private lateinit var sleepExporter: SleepExporterImpl

    @Autowired
    private lateinit var sleepRepository: SleepRepository

    @Disabled("Only run manually, requires database with data")
    @Test
    fun `test export returns sleep data`() {
        val from = LocalDateTime.of(2024, 1, 1, 0, 0)
        val to = LocalDateTime.of(2024, 12, 31, 23, 59)
        val output = ByteArrayOutputStream()

        val count = sleepExporter.export(from, to, output)

        println("Exported $count sleep level records")
        println("XML size: ${output.size()} bytes")
    }

    @Test
    fun `test mapFitbitLevelToAppleHealth maps wake correctly`() {
        assertEquals(2, sleepExporter.mapFitbitLevelToAppleHealth("wake"))
        assertEquals(2, sleepExporter.mapFitbitLevelToAppleHealth("awake"))
        assertEquals(2, sleepExporter.mapFitbitLevelToAppleHealth("WAKE"))
    }

    @Test
    fun `test mapFitbitLevelToAppleHealth maps light to Core`() {
        assertEquals(3, sleepExporter.mapFitbitLevelToAppleHealth("light"))
        assertEquals(3, sleepExporter.mapFitbitLevelToAppleHealth("LIGHT"))
    }

    @Test
    fun `test mapFitbitLevelToAppleHealth maps deep correctly`() {
        assertEquals(4, sleepExporter.mapFitbitLevelToAppleHealth("deep"))
        assertEquals(4, sleepExporter.mapFitbitLevelToAppleHealth("DEEP"))
    }

    @Test
    fun `test mapFitbitLevelToAppleHealth maps rem correctly`() {
        assertEquals(5, sleepExporter.mapFitbitLevelToAppleHealth("rem"))
        assertEquals(5, sleepExporter.mapFitbitLevelToAppleHealth("REM"))
    }

    @Test
    fun `test mapFitbitLevelToAppleHealth maps asleep correctly`() {
        assertEquals(1, sleepExporter.mapFitbitLevelToAppleHealth("asleep"))
    }

    @Test
    fun `test mapFitbitLevelToAppleHealth maps unknown to InBed`() {
        assertEquals(0, sleepExporter.mapFitbitLevelToAppleHealth("unknown"))
        assertEquals(0, sleepExporter.mapFitbitLevelToAppleHealth("restless"))
    }

    @Test
    fun `test toRecord maps sleep level data correctly`() {
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
            mainSleep = true
        )

        val sleepLevelData = SleepLevelData(
            dateTime = LocalDateTime.of(2024, 6, 14, 22, 15, 0),
            level = "deep",
            seconds = 1800,
            sleep = sleep
        )

        val record = sleepExporter.toRecord(sleepLevelData)

        assertEquals("HKCategoryTypeIdentifierSleepAnalysis", record.type)
        assertEquals("Fitbit-dump", record.sourceName)
        assertEquals("", record.unit)  // Category types don't have units
        assertEquals("4", record.value)  // deep = 4
        assertTrue(record.startDate.contains("2024-06-14"))
        assertTrue(record.endDate.contains("2024-06-14"))
    }

    @Test
    fun `test toRecord calculates end time correctly based on seconds`() {
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
            mainSleep = true
        )

        val sleepLevelData = SleepLevelData(
            dateTime = LocalDateTime.of(2024, 6, 14, 23, 0, 0),
            level = "light",
            seconds = 3600,  // 1 hour
            sleep = sleep
        )

        val record = sleepExporter.toRecord(sleepLevelData)

        // Start is 23:00, end should be 00:00 (next day)
        assertTrue(record.startDate.contains("23:00:00"))
        assertTrue(record.endDate.contains("00:00:00"))
    }

    @Test
    fun `test healthKitType returns correct identifier`() {
        assertEquals("HKCategoryTypeIdentifierSleepAnalysis", sleepExporter.healthKitType())
    }

    @Test
    fun `test unit returns empty string for category type`() {
        assertEquals("", sleepExporter.unit())
    }

    @Disabled("Requires database setup")
    @Test
    fun `test export generates valid XML structure`() {
        val sleep = Sleep(
            logId = System.currentTimeMillis(),
            dateOfSleep = LocalDateTime.of(2024, 1, 1, 0, 0),
            startTime = LocalDateTime.of(2024, 1, 1, 22, 0),
            endTime = LocalDateTime.of(2024, 1, 2, 6, 0),
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
            mainSleep = true
        )

        val sleepLevelData = SleepLevelData(
            dateTime = LocalDateTime.of(2024, 1, 1, 22, 30, 0),
            level = "rem",
            seconds = 1200,
            sleep = sleep
        )
        sleep.levelData.add(sleepLevelData)
        sleepRepository.save(sleep)

        val from = LocalDateTime.of(2024, 1, 1, 0, 0)
        val to = LocalDateTime.of(2024, 1, 2, 23, 59)
        val output = ByteArrayOutputStream()

        val count = sleepExporter.export(from, to, output)

        assertTrue(count >= 1)
        val xml = output.toString("UTF-8")
        assertTrue(xml.contains("<?xml version=\"1.0\""))
        assertTrue(xml.contains("<HealthData"))
        assertTrue(xml.contains("HKCategoryTypeIdentifierSleepAnalysis"))
    }
}
