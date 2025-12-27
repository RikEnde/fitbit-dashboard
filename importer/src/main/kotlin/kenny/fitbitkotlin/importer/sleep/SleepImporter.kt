package kenny.fitbitkotlin.importer.sleep

import kenny.fitbitkotlin.importer.Importer
import kenny.fitbitkotlin.sleep.*
import java.io.File

interface SleepImporter : Importer<Sleep> {
    override fun directory(): String = "Sleep"
    override fun filePattern(): String = "sleep-\\d{4}-\\d{2}-\\d{2}\\.json"
}

interface DeviceTemperatureImporter : Importer<DeviceTemperature> {
    override fun directory(): String = "Sleep"
    override fun filePattern(): String = "Device Temperature - \\d{4}-\\d{2}-\\d{2}\\.csv"
    override fun import(): Int
    suspend fun importFile(index: Int, size: Int, file: File)
}

interface MinuteSpO2Importer : Importer<MinuteSpO2> {
    override fun directory(): String = "Sleep"
    override fun filePattern(): String = "Minute SpO2 - \\d{4}-\\d{2}-\\d{2}\\.csv"
    override fun import(): Int
    suspend fun importFile(index: Int, size: Int, file: File)
}

interface DailySpO2Importer : Importer<DailySpO2> {
    override fun directory(): String = "Sleep"
    override fun filePattern(): String = "Daily SpO2 - \\d{4}-\\d{2}-\\d{2}-\\d{4}-\\d{2}-\\d{2}\\.csv"
    override fun import(): Int
    suspend fun importFile(index: Int, size: Int, file: File)
}

interface DailyRespiratoryRateImporter : Importer<DailyRespiratoryRate> {
    override fun directory(): String = "Sleep"
    override fun filePattern(): String = "Daily Respiratory Rate Summary - \\d{4}-\\d{2}-\\d{2}\\.csv"
    override fun import(): Int
    suspend fun importFile(index: Int, size: Int, file: File)
}

interface ComputedTemperatureImporter : Importer<ComputedTemperature> {
    override fun directory(): String = "Sleep"
    override fun filePattern(): String = "Computed Temperature - \\d{4}-\\d{2}-\\d{2}\\.csv"
    override fun import(): Int
    suspend fun importFile(index: Int, size: Int, file: File)
}

interface RespiratoryRateSummaryImporter : Importer<RespiratoryRateSummary> {
    override fun directory(): String = "Sleep"
    override fun filePattern(): String = "Respiratory Rate Summary - \\d{4}-\\d{2}-\\d{2}\\.csv"
    override fun import(): Int
    suspend fun importFile(index: Int, size: Int, file: File)
}

interface SleepScoreImporter : Importer<SleepScore> {
    override fun directory(): String = "Sleep"
    override fun filePattern(): String = "sleep_score\\.csv"
    override fun import(): Int
    suspend fun importFile(index: Int, size: Int, file: File)
}
