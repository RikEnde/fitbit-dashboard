package kenny.fitbit.sleep

import kenny.fitbit.Importer

interface SleepImporter : Importer<Sleep> {
    override fun directory(): String = "Sleep"
    override fun filePattern(): String = "sleep-\\d{4}-\\d{2}-\\d{2}\\.json"
}

interface DeviceTemperatureImporter : Importer<DeviceTemperature> {
    override fun directory(): String = "Sleep"
    override fun filePattern(): String = "Device Temperature - \\d{4}-\\d{2}-\\d{2}\\.csv"
}

interface MinuteSpO2Importer : Importer<MinuteSpO2> {
    override fun directory(): String = "Sleep"
    override fun filePattern(): String = "Minute SpO2 - \\d{4}-\\d{2}-\\d{2}\\.csv"
}

interface DailySpO2Importer : Importer<DailySpO2> {
    override fun directory(): String = "Sleep"
    override fun filePattern(): String = "Daily SpO2 - \\d{4}-\\d{2}-\\d{2}-\\d{4}-\\d{2}-\\d{2}\\.csv"
}

interface DailyRespiratoryRateImporter : Importer<DailyRespiratoryRate> {
    override fun directory(): String = "Sleep"
    override fun filePattern(): String = "Daily Respiratory Rate Summary - \\d{4}-\\d{2}-\\d{2}\\.csv"
}

interface ComputedTemperatureImporter : Importer<ComputedTemperature> {
    override fun directory(): String = "Sleep"
    override fun filePattern(): String = "Computed Temperature - \\d{4}-\\d{2}-\\d{2}\\.csv"
}

interface RespiratoryRateSummaryImporter : Importer<RespiratoryRateSummary> {
    override fun directory(): String = "Sleep"
    override fun filePattern(): String = "Respiratory Rate Summary - \\d{4}-\\d{2}-\\d{2}\\.csv"
}

interface SleepScoreImporter : Importer<SleepScore> {
    override fun directory(): String = "Sleep"
    override fun filePattern(): String = "sleep_score\\.csv"
}
