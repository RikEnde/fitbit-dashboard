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
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

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

enum class ImportStatus { RUNNING, COMPLETED, FAILED }

data class ImportJob(
    val jobId: String,
    @Volatile var status: ImportStatus = ImportStatus.RUNNING,
    @Volatile var message: String? = null,
    @Volatile var results: ImportResponse? = null,
    @Volatile var error: String? = null,
    val createdAt: Instant = Instant.now()
)

data class ImportJobResponse(
    val jobId: String,
    val status: ImportStatus,
    val message: String?,
    val results: ImportResponse?,
    val error: String?
)

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

    private val jobs = ConcurrentHashMap<String, ImportJob>()

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
    fun importData(@RequestBody request: ImportRequest): ResponseEntity<Map<String, String>> {
        // Clean up jobs older than 1 hour
        val cutoff = Instant.now().minusSeconds(3600)
        jobs.entries.removeIf { it.value.createdAt.isBefore(cutoff) }

        val job = ImportJob(jobId = UUID.randomUUID().toString())
        jobs[job.jobId] = job

        Thread {
            try {
                val results = mutableListOf<UserResult>()

                for (user in request.users) {
                    allImporters.forEach {
                        it.dataDir = request.dataDir
                        it.userDir = user
                        it.onProgress = { message -> job.message = message }
                    }

                    accountImporter.import()
                    val profile = accountImporter.profile
                    if (profile == null) {
                        job.message = "WARNING: No profile imported for user $user, skipping stats"
                        continue
                    }

                    allImporters.forEach { it.profile = profile }

                    val statsResults = mutableMapOf<String, StatResult>()
                    val requestedStats = if (request.stats.contains("all")) statImporters.keys else request.stats

                    for (statType in requestedStats) {
                        val importer = statImporters[statType]
                        if (importer == null) {
                            job.message = "Unknown stat type: $statType"
                            continue
                        }

                        job.message = "Importing $statType data for user $user..."
                        val count = importer.import()
                        job.message = "Imported $count $statType files"

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

                job.results = ImportResponse(results = results)
                job.status = ImportStatus.COMPLETED
            } catch (e: Exception) {
                job.error = e.message ?: "Unknown error"
                job.status = ImportStatus.FAILED
            } finally {
                allImporters.forEach { it.onProgress = ::println }
            }
        }.start()

        return ResponseEntity.ok(mapOf("jobId" to job.jobId))
    }

    @GetMapping("/{jobId}")
    fun getJobStatus(@PathVariable jobId: String): ResponseEntity<ImportJobResponse> {
        val job = jobs[jobId] ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(
            ImportJobResponse(
                jobId = job.jobId,
                status = job.status,
                message = job.message,
                results = job.results,
                error = job.error
            )
        )
    }
}
