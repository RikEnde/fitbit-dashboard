package kenny.fitbitkotlin.exercise

import kenny.fitbitkotlin.Importer
import kenny.fitbitkotlin.heartrate.TimeInHeartRateZones
import java.io.File

interface TimeInHeartRateZonesImporter : Importer<TimeInHeartRateZones> {
    override fun directory(): String = "Physical Activity"
    override fun filePattern(): String = "time_in_heart_rate_zones-.*\\.json"
}

interface ActivityGoalImporter : Importer<ActivityGoal> {
    override fun directory(): String = "Physical Activity"
    override fun filePattern(): String = "Activity Goals\\.csv"
    override fun import(): Int
    suspend fun importFile(index: Int, size: Int, file: File)
}

interface ExerciseImporter : Importer<Exercise> {
    override fun directory(): String = "Physical Activity"
    override fun filePattern(): String = "exercise-\\d+.json"
}

interface ActivityMinutesImporter : Importer<ActivityMinutes> {
    override fun directory(): String = "Physical Activity"
    override fun filePattern(): String =
        "(sedentary|lightly_active|moderately_active|very_active)_minutes-\\d{4}-\\d{2}-\\d{2}\\.json"
}

interface ActiveZoneMinutesImporter : Importer<ActivityMinutes> {
    override fun directory(): String = "Physical Activity"
    override fun filePattern(): String = "Active Zone Minutes - \\d{4}-\\d{2}-\\d{2}\\.csv"
    override fun import(): Int
    suspend fun importFile(index: Int, size: Int, file: File)
}

interface DemographicVO2MaxImporter : Importer<DemographicVO2Max> {
    override fun directory(): String = "Physical Activity"
    override fun filePattern(): String = "demographic_vo2_max-\\d{4}-\\d{2}-\\d{2}\\.json"
}

interface RunVO2MaxImporter : Importer<RunVO2Max> {
    override fun directory(): String = "Physical Activity"
    override fun filePattern(): String = "run_vo2_max-\\d{4}-\\d{2}-\\d{2}\\.json"
}
