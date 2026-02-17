package kenny.fitbit

import kenny.fitbit.calories.CaloriesImporter
import kenny.fitbit.distance.DistanceImporter
import kenny.fitbit.exercise.*
import kenny.fitbit.heartrate.DailyHeartRateVariabilityImporter
import kenny.fitbit.heartrate.HeartRateImporterImpl
import kenny.fitbit.heartrate.HeartRateVariabilityDetailsImporter
import kenny.fitbit.heartrate.RestingHeartRateImporter
import kenny.fitbit.importlog.ImportLog
import kenny.fitbit.importlog.ImportLogRepository
import kenny.fitbit.profile.AccountImporter
import kenny.fitbit.sleep.*
import kenny.fitbit.steps.StepsImporter
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

data class ImportRequest(
    val dataDir: String = "../data",
    val users: List<String> = emptyList(),
    val stats: List<String> = emptyList()
)

data class StatResult(val fileCount: Int)

data class UserResult(
    val user: String,
    val stats: Map<String, StatResult>
)

data class ImportResponse(val results: List<UserResult>)

@RestController
@RequestMapping("/api/import")
class ImportController(
    private val heartRateImporter: HeartRateImporterImpl,
    private val stepsImporter: StepsImporter,
    private val caloriesImporter: CaloriesImporter,
    private val distanceImporter: DistanceImporter,
    private val restingHeartRateImporter: RestingHeartRateImporter,
    private val exerciseImporter: ExerciseImporter,
    private val timeInHeartRateZonesImporter: TimeInHeartRateZonesImporter,
    private val activityMinutesImporter: ActivityMinutesImporter,
    private val activeZoneMinutesImporter: ActiveZoneMinutesImporter,
    private val demographicVO2MaxImporter: DemographicVO2MaxImporter,
    private val runVO2MaxImporter: RunVO2MaxImporter,
    private val activityGoalImporter: ActivityGoalImporter,
    private val sleepImporter: SleepImporter,
    private val deviceTemperatureImporter: DeviceTemperatureImporter,
    private val dailyRespiratoryRateImporter: DailyRespiratoryRateImporter,
    private val dailyHeartRateVariabilityImporter: DailyHeartRateVariabilityImporter,
    private val heartRateVariabilityDetailsImporter: HeartRateVariabilityDetailsImporter,
    private val minuteSpO2Importer: MinuteSpO2Importer,
    private val sleepScoreImporter: SleepScoreImporter,
    private val computedTemperatureImporter: ComputedTemperatureImporter,
    private val respiratoryRateSummaryImporter: RespiratoryRateSummaryImporter,
    private val dailySpO2Importer: DailySpO2Importer,
    private val accountImporter: AccountImporter,
    private val importLogRepository: ImportLogRepository
) {

    private val statImporters: Map<String, Importer<*>> by lazy {
        mapOf(
            "heartrate" to heartRateImporter,
            "steps" to stepsImporter,
            "calories" to caloriesImporter,
            "distance" to distanceImporter,
            "restingheartrate" to restingHeartRateImporter,
            "exercise" to exerciseImporter,
            "timeinzone" to timeInHeartRateZonesImporter,
            "activityminutes" to activityMinutesImporter,
            "activezoneminutes" to activeZoneMinutesImporter,
            "vo2max" to demographicVO2MaxImporter,
            "runvo2max" to runVO2MaxImporter,
            "activitygoals" to activityGoalImporter,
            "sleep" to sleepImporter,
            "devicetemperature" to deviceTemperatureImporter,
            "respiratoryrate" to dailyRespiratoryRateImporter,
            "hrv" to dailyHeartRateVariabilityImporter,
            "hrvdetails" to heartRateVariabilityDetailsImporter,
            "minutespo2" to minuteSpO2Importer,
            "sleepscore" to sleepScoreImporter,
            "computedtemperature" to computedTemperatureImporter,
            "respiratoryratesummary" to respiratoryRateSummaryImporter,
            "dailyspo2" to dailySpO2Importer
        )
    }

    private val allImporters: List<Importer<*>>
        get() = statImporters.values.toList() + accountImporter

    @PostMapping
    @Synchronized
    fun importData(@RequestBody request: ImportRequest): ResponseEntity<ImportResponse> {
        val results = mutableListOf<UserResult>()

        for (user in request.users) {
            // Set dataDir and userDir on all importers
            allImporters.forEach {
                it.dataDir = request.dataDir
                it.userDir = user
            }

            // Import profile first
            val profileCount = accountImporter.import()
            val profile = accountImporter.profile
            if (profile == null) {
                println("WARNING: No profile imported for user $user, skipping stats")
                continue
            }

            // Set profile on all importers
            allImporters.forEach { it.profile = profile }

            // Import requested stats
            val statsResults = mutableMapOf<String, StatResult>()
            val requestedStats = if (request.stats.contains("all")) statImporters.keys else request.stats

            for (statType in requestedStats) {
                val importer = statImporters[statType]
                if (importer == null) {
                    println("Unknown stat type: $statType")
                    continue
                }

                println("Importing $statType data for user $user...")
                val count = importer.import()
                println("Imported $count $statType files")

                if (count > 0 && importer.maxDate != null) {
                    importLogRepository.save(
                        ImportLog(
                            profile = profile,
                            statType = statType,
                            latestDataDate = importer.maxDate!!,
                            importedAt = LocalDateTime.now(),
                            fileCount = count
                        )
                    )
                }

                statsResults[statType] = StatResult(fileCount = count)
            }

            results.add(UserResult(user = user, stats = statsResults))
        }

        return ResponseEntity.ok(ImportResponse(results = results))
    }
}
