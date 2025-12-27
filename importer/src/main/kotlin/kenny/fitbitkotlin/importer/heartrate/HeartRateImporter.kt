package kenny.fitbitkotlin.importer.heartrate

import kenny.fitbitkotlin.importer.Importer
import kenny.fitbitkotlin.heartrate.HeartRate
import kenny.fitbitkotlin.heartrate.RestingHeartRate
import kenny.fitbitkotlin.heartrate.DailyHeartRateVariability
import kenny.fitbitkotlin.heartrate.HeartRateVariabilityDetails
import java.io.File

interface HeartRateImporter : Importer<HeartRate> {
    override fun directory(): String = "Physical Activity"
    override fun filePattern(): String = "heart_rate-\\d{4}-\\d{2}-\\d{2}.json"
}

interface RestingHeartRateImporter : Importer<RestingHeartRate> {
    override fun directory(): String = "Physical Activity"
    override fun filePattern(): String = "resting_heart_rate-\\d{4}-\\d{2}-\\d{2}.json"
}

interface DailyHeartRateVariabilityImporter : Importer<DailyHeartRateVariability> {
    override fun directory(): String = "Sleep"
    override fun filePattern(): String = "Daily Heart Rate Variability Summary - \\d{4}-\\d{2}-\\d{2}\\.csv"
    override fun import(): Int
    suspend fun importFile(index: Int, size: Int, file: File)
}

interface HeartRateVariabilityDetailsImporter : Importer<HeartRateVariabilityDetails> {
    override fun directory(): String = "Sleep"
    override fun filePattern(): String = "Heart Rate Variability Details - \\d{4}-\\d{2}-\\d{2}\\.csv"
    override fun import(): Int
    suspend fun importFile(index: Int, size: Int, file: File)
}
