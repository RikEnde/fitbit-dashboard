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
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.nio.file.Files
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.ZipInputStream

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
    private val importLogRepository: ImportLogRepository,
    private val authenticatedProfileService: AuthenticatedProfileService,
    private val profileRepository: kenny.fitbit.profile.ProfileRepository
) {

    private val log = LoggerFactory.getLogger(javaClass)
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
    fun uploadZip(
        @RequestParam("file") file: MultipartFile,
        @RequestParam("stats", defaultValue = "all") stats: List<String>
    ): ResponseEntity<Any> {
        val originalFilename = file.originalFilename ?: "upload"
        if (!originalFilename.endsWith(".zip", ignoreCase = true)) {
            return ResponseEntity.badRequest().body(mapOf("error" to "Only .zip files are accepted"))
        }

        val freeSpace = File(System.getProperty("java.io.tmpdir")).usableSpace
        val requiredSpace = file.size * 15 // zip expands ~10-15x
        if (freeSpace < requiredSpace) {
            return ResponseEntity.badRequest().body(
                mapOf("error" to "Insufficient disk space. Need ~${requiredSpace / 1_000_000_000}GB free, have ${freeSpace / 1_000_000_000}GB")
            )
        }

        val job = createJob()
        val tempDir = Files.createTempDirectory("fitbit-import-${job.jobId}").toFile()

        // Capture authenticated username before spawning background thread (SecurityContext is thread-local)
        val authenticatedUsername = org.springframework.security.core.context.SecurityContextHolder.getContext().authentication.name

        // Save file before returning — Spring cleans up MultipartFile after the request completes
        val zipFile = File(tempDir, "upload.zip")
        file.transferTo(zipFile)

        Thread {
            try {
                job.message = "Extracting zip file..."
                val extractDir = File(tempDir, "extracted")
                extractDir.mkdirs()
                extractZip(zipFile, extractDir)
                zipFile.delete()

                val (userName, dataDir) = detectUserDir(extractDir)
                    ?: throw IllegalStateException("Could not detect user directory in zip. Expected a top-level folder containing subdirectories like 'Physical Activity', 'Sleep', etc.")

                job.message = "Detected user: $userName"

                val userResult = importUser(userName, dataDir, stats, job, authenticatedUsername)
                val results = if (userResult != null) listOf(userResult) else emptyList()

                job.results = ImportResponse(results = results)
                job.status = ImportStatus.COMPLETED
            } catch (e: Exception) {
                log.error("Import failed for job ${job.jobId}", e)
                job.error = if (e is IllegalStateException) e.message ?: "Import failed" else "Import failed"
                job.status = ImportStatus.FAILED
            } finally {
                allImporters.forEach { it.onProgress = ::println }
                tempDir.deleteRecursively()
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

    private fun createJob(): ImportJob {
        val cutoff = Instant.now().minusSeconds(3600)
        jobs.entries.removeIf { it.value.createdAt.isBefore(cutoff) }

        val job = ImportJob(jobId = UUID.randomUUID().toString())
        jobs[job.jobId] = job
        return job
    }

    private fun importUser(user: String, dataDir: String, stats: List<String>, job: ImportJob, authenticatedUsername: String): UserResult? {
        allImporters.forEach {
            it.dataDir = dataDir
            it.userDir = user
            it.onProgress = { message -> job.message = message }
        }

        accountImporter.import()
        val profile = accountImporter.profile
        if (profile == null) {
            job.message = "WARNING: No profile imported for user $user, skipping stats"
            return null
        }

        // Set profile username to the authenticated user's login username so that
        // AuthenticatedProfileService can find the profile regardless of the zip directory name
        val savedProfile = profileRepository.save(profile.copy(username = authenticatedUsername))

        allImporters.forEach { it.profile = savedProfile }

        val statsResults = mutableMapOf<String, StatResult>()
        val requestedStats = if (stats.contains("all")) statImporters.keys else stats

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
                        profile = savedProfile,
                        statType = statType,
                        latestDataDate = importer.maxDate!!,
                        importedAt = LocalDateTime.now(),
                        fileCount = count
                    )
                )
            }

            statsResults[statType] = StatResult(fileCount = count)
        }

        return UserResult(user = user, stats = statsResults)
    }

    private fun extractZip(zipFile: File, destDir: File) {
        val destPath = destDir.toPath().toRealPath()

        ZipInputStream(zipFile.inputStream().buffered()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val resolvedPath = destPath.resolve(entry.name).normalize()
                if (!resolvedPath.startsWith(destPath)) {
                    throw SecurityException("Zip entry '${entry.name}' is outside of the target directory (path traversal)")
                }

                if (entry.isDirectory) {
                    Files.createDirectories(resolvedPath)
                } else {
                    Files.createDirectories(resolvedPath.parent)
                    Files.newOutputStream(resolvedPath).use { out ->
                        zis.copyTo(out)
                    }
                }

                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }

    /** Returns (userName, dataDir) where dataDir is the parent of the user directory. */
    private fun detectUserDir(extractDir: File): Pair<String, String>? {
        val knownSubdirs = setOf(
            "Physical Activity", "Sleep", "Heart Rate Variability",
            "Stress", "Body", "Food and Water", "Mindfulness",
            "Estimated Oxygen Variation", "Temperature",
            "heart_rate", "steps", "calories", "distance", "exercise",
            "sleep", "hrv", "spo2", "temperature"
        )

        fun hasKnownSubdirs(dir: File): Boolean {
            val children = dir.listFiles()?.filter { it.isDirectory }?.map { it.name } ?: return false
            return children.any { it in knownSubdirs }
        }

        val directChildren = extractDir.listFiles()?.filter { it.isDirectory } ?: return null

        // Level 0: extractDir itself contains known subdirs (files at root of zip)
        if (hasKnownSubdirs(extractDir)) {
            return extractDir.name to extractDir.parent
        }

        for (child in directChildren) {
            // Level 1: child is the user dir (e.g. extracted/YourName/Physical Activity/)
            if (hasKnownSubdirs(child)) {
                return child.name to extractDir.absolutePath
            }
            // Level 2: zip wrapper dir → user dir (e.g. extracted/MyFitbitData-3/YourName/Physical Activity/)
            val grandchildren = child.listFiles()?.filter { it.isDirectory } ?: continue
            for (grandchild in grandchildren) {
                if (hasKnownSubdirs(grandchild)) {
                    return grandchild.name to child.absolutePath
                }
            }
        }

        return null
    }
}
