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
import kenny.fitbit.profile.Profile
import kenny.fitbit.sleep.*
import kenny.fitbit.steps.StepsImporter
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.stereotype.Component
import java.io.File
import java.time.LocalDateTime

@SpringBootApplication
@EntityScan(basePackages = ["kenny.fitbit"])
@EnableJpaRepositories(basePackages = ["kenny.fitbit"])
class FitbitImporterApplication

fun main(args: Array<String>) {
    runApplication<FitbitImporterApplication>(*args)
}

@Component
class ImportRunner(
    val heartRateImporter: HeartRateImporterImpl,
    val stepsImporter: StepsImporter,
    val caloriesImporter: CaloriesImporter,
    val distanceImporter: DistanceImporter,
    val restingHeartRateImporter: RestingHeartRateImporter,
    val exerciseImporter: ExerciseImporter,
    val timeInHeartRateZonesImporter: TimeInHeartRateZonesImporter,
    val activityMinutesImporter: ActivityMinutesImporter,
    val activeZoneMinutesImporter: ActiveZoneMinutesImporter,
    val demographicVO2MaxImporter: DemographicVO2MaxImporter,
    val runVO2MaxImporter: RunVO2MaxImporter,
    val activityGoalImporter: ActivityGoalImporter,
    val sleepImporter: SleepImporter,
    val deviceTemperatureImporter: DeviceTemperatureImporter,
    val dailyRespiratoryRateImporter: DailyRespiratoryRateImporter,
    val dailyHeartRateVariabilityImporter: DailyHeartRateVariabilityImporter,
    val heartRateVariabilityDetailsImporter: HeartRateVariabilityDetailsImporter,
    val minuteSpO2Importer: MinuteSpO2Importer,
    val sleepScoreImporter: SleepScoreImporter,
    val computedTemperatureImporter: ComputedTemperatureImporter,
    val respiratoryRateSummaryImporter: RespiratoryRateSummaryImporter,
    val dailySpO2Importer: DailySpO2Importer,
    val accountImporter: AccountImporter,
    val importLogRepository: ImportLogRepository
) : ApplicationRunner {

    private val allImporters: List<Importer<*>>
        get() = listOf(
            heartRateImporter, stepsImporter, caloriesImporter, distanceImporter,
            restingHeartRateImporter, exerciseImporter, timeInHeartRateZonesImporter,
            activityMinutesImporter, activeZoneMinutesImporter, demographicVO2MaxImporter,
            runVO2MaxImporter, activityGoalImporter, sleepImporter, deviceTemperatureImporter,
            dailyRespiratoryRateImporter, dailyHeartRateVariabilityImporter,
            heartRateVariabilityDetailsImporter, minuteSpO2Importer, sleepScoreImporter,
            computedTemperatureImporter, respiratoryRateSummaryImporter, dailySpO2Importer,
            accountImporter
        )

    override fun run(args: ApplicationArguments) {
        val all = args.containsOption("all")
        fun hasOption(option: String) = all || args.containsOption(option)

        val dataDir = args.getOptionValues("datadir")?.firstOrNull() ?: "../data"

        // Scan data directory for user directories
        val dataDirFile = File(dataDir)
        val selectedUser = args.getOptionValues("user")?.firstOrNull()
        val userDirs = dataDirFile.listFiles { file -> file.isDirectory }
            ?.map { it.name }
            ?.filter { selectedUser == null || it == selectedUser }
            ?.sorted()
            ?: emptyList()

        if (userDirs.isEmpty()) {
            if (selectedUser != null) {
                println("User directory '$selectedUser' not found in $dataDir")
            } else {
                println("No user directories found in $dataDir")
            }
            return
        }

        println("Found ${userDirs.size} user directories: $userDirs")

        for (userDirName in userDirs) {
            println("\n=== Importing data for user: $userDirName ===")

            // Set dataDir and userDir on all importers
            allImporters.forEach {
                it.dataDir = dataDir
                it.userDir = userDirName
            }

            // Always import profile first to establish the profile reference
            println("Importing profile data...")
            val profileCount = accountImporter.import()
            println("Imported $profileCount profile files")

            val profile = accountImporter.profile
            if (profile == null) {
                println("WARNING: No profile imported for user $userDirName, skipping stats")
                continue
            }

            // Set profile on all importers
            allImporters.forEach { it.profile = profile }

            // Import each stat type and log results
            importStat("heartrate", hasOption("heartrate"), heartRateImporter, profile)
            importStat("steps", hasOption("steps"), stepsImporter, profile)
            importStat("calories", hasOption("calories"), caloriesImporter, profile)
            importStat("distance", hasOption("distance"), distanceImporter, profile)
            importStat("restingheartrate", hasOption("restingheartrate"), restingHeartRateImporter, profile)
            importStat("exercise", hasOption("exercise"), exerciseImporter, profile)
            importStat("timeinzone", hasOption("timeinzone"), timeInHeartRateZonesImporter, profile)
            importStat("activityminutes", hasOption("activityminutes"), activityMinutesImporter, profile)
            importStat("activezoneminutes", hasOption("activezoneminutes"), activeZoneMinutesImporter, profile)
            importStat("vo2max", hasOption("vo2max"), demographicVO2MaxImporter, profile)
            importStat("runvo2max", hasOption("runvo2max"), runVO2MaxImporter, profile)
            importStat("activitygoals", hasOption("activitygoals"), activityGoalImporter, profile)
            importStat("sleep", hasOption("sleep"), sleepImporter, profile)
            importStat("devicetemperature", hasOption("devicetemperature"), deviceTemperatureImporter, profile)
            importStat("respiratoryrate", hasOption("respiratoryrate"), dailyRespiratoryRateImporter, profile)
            importStat("hrv", hasOption("hrv"), dailyHeartRateVariabilityImporter, profile)
            importStat("hrvdetails", hasOption("hrvdetails"), heartRateVariabilityDetailsImporter, profile)
            importStat("minutespo2", hasOption("minutespo2"), minuteSpO2Importer, profile)
            importStat("sleepscore", hasOption("sleepscore"), sleepScoreImporter, profile)
            importStat("computedtemperature", hasOption("computedtemperature"), computedTemperatureImporter, profile)
            importStat("respiratoryratesummary", hasOption("respiratoryratesummary"), respiratoryRateSummaryImporter, profile)
            importStat("dailyspo2", hasOption("dailyspo2"), dailySpO2Importer, profile)
        }
    }

    private fun importStat(statType: String, enabled: Boolean, importer: Importer<*>, profile: Profile) {
        if (!enabled) return

        println("Importing $statType data...")
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
    }
}
