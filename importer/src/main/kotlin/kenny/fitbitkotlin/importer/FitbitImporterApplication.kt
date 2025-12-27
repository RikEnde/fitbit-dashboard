package kenny.fitbitkotlin.importer

import kenny.fitbitkotlin.importer.calories.CaloriesImporter
import kenny.fitbitkotlin.importer.distance.DistanceImporter
import kenny.fitbitkotlin.importer.exercise.*
import kenny.fitbitkotlin.importer.heartrate.*
import kenny.fitbitkotlin.importer.profile.AccountImporter
import kenny.fitbitkotlin.importer.sleep.*
import kenny.fitbitkotlin.importer.steps.StepsImporter
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.stereotype.Component

@SpringBootApplication
@ComponentScan(basePackages = ["kenny.fitbitkotlin"])
@EntityScan(basePackages = ["kenny.fitbitkotlin"])
@EnableJpaRepositories(basePackages = ["kenny.fitbitkotlin"])
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
    val accountImporter: AccountImporter
) : ApplicationRunner {
    override fun run(args: ApplicationArguments) {
        if (args.containsOption("heartrate")) {
            println("Importing heart rate data...")
            val count = heartRateImporter.import()
            println("Imported $count heart rate files")
        }

        if (args.containsOption("steps")) {
            println("Importing steps data...")
            val count = stepsImporter.import()
            println("Imported $count steps files")
        }

        if (args.containsOption("calories")) {
            println("Importing calories data...")
            val count = caloriesImporter.import()
            println("Imported $count calories files")
        }

        if (args.containsOption("distance")) {
            println("Importing distance data...")
            val count = distanceImporter.import()
            println("Imported $count distance files")
        }

        if (args.containsOption("restingheartrate")) {
            println("Importing resting heart rate data...")
            val count = restingHeartRateImporter.import()
            println("Imported $count resting heart rate files")
        }

        if (args.containsOption("exercise")) {
            println("Importing exercise data...")
            val count = exerciseImporter.import()
            println("Imported $count exercise files")
        }

        if (args.containsOption("timeinzone")) {
            println("Importing timeinzone data...")
            val count = timeInHeartRateZonesImporter.import()
            println("Imported $count timeinzone files")
        }

        if (args.containsOption("activityminutes")) {
            println("Importing activity minutes data...")
            val count = activityMinutesImporter.import()
            println("Imported $count activity minutes files")
        }

        if (args.containsOption("activezoneminutes")) {
            println("Importing active zone minutes data...")
            val count = activeZoneMinutesImporter.import()
            println("Imported $count active zone minutes files")
        }

        if (args.containsOption("vo2max")) {
            println("Importing demographic VO2 max data...")
            val count = demographicVO2MaxImporter.import()
            println("Imported $count demographic VO2 max files")
        }

        if (args.containsOption("runvo2max")) {
            println("Importing run VO2 max data...")
            val count = runVO2MaxImporter.import()
            println("Imported $count run VO2 max files")
        }

        if (args.containsOption("activitygoals")) {
            println("Importing activity goals data...")
            val count = activityGoalImporter.import()
            println("Imported $count activity goals files")
        }

        if (args.containsOption("sleep")) {
            println("Importing sleep data...")
            val count = sleepImporter.import()
            println("Imported $count sleep files")
        }

        if (args.containsOption("devicetemperature")) {
            println("Importing device temperature data...")
            val count = deviceTemperatureImporter.import()
            println("Imported $count device temperature files")
        }

        if (args.containsOption("respiratoryrate")) {
            println("Importing daily respiratory rate data...")
            val count = dailyRespiratoryRateImporter.import()
            println("Imported $count daily respiratory rate files")
        }

        if (args.containsOption("hrv")) {
            println("Importing daily heart rate variability data...")
            val count = dailyHeartRateVariabilityImporter.import()
            println("Imported $count daily heart rate variability files")
        }

        if (args.containsOption("hrvdetails")) {
            println("Importing heart rate variability details data...")
            val count = heartRateVariabilityDetailsImporter.import()
            println("Imported $count heart rate variability details files")
        }

        if (args.containsOption("minutespo2")) {
            println("Importing minute SpO2 data...")
            val count = minuteSpO2Importer.import()
            println("Imported $count minute SpO2 files")
        }

        if (args.containsOption("sleepscore")) {
            println("Importing sleep score data...")
            val count = sleepScoreImporter.import()
            println("Imported $count sleep score files")
        }

        if (args.containsOption("computedtemperature")) {
            println("Importing computed temperature data...")
            val count = computedTemperatureImporter.import()
            println("Imported $count computed temperature files")
        }

        if (args.containsOption("respiratoryratesummary")) {
            println("Importing respiratory rate summary data...")
            val count = respiratoryRateSummaryImporter.import()
            println("Imported $count respiratory rate summary files")
        }

        if (args.containsOption("dailyspo2")) {
            println("Importing daily SpO2 data...")
            val count = dailySpO2Importer.import()
            println("Imported $count daily SpO2 files")
        }

        if (args.containsOption("profile")) {
            println("Importing profile data...")
            val count = accountImporter.import()
            println("Imported $count profile files")
        }
    }
}
