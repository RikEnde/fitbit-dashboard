package kenny.fitbit.heartrate

import kenny.fitbit.Importer

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
}

interface HeartRateVariabilityDetailsImporter : Importer<HeartRateVariabilityDetails> {
    override fun directory(): String = "Sleep"
    override fun filePattern(): String = "Heart Rate Variability Details - \\d{4}-\\d{2}-\\d{2}\\.csv"
}
