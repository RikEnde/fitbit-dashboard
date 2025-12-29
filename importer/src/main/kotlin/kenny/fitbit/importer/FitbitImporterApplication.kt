package kenny.fitbit.importer

import kenny.fitbit.importer.calories.CaloriesImporter
import kenny.fitbit.importer.distance.DistanceImporter
import kenny.fitbit.importer.exercise.*
import kenny.fitbit.importer.heartrate.DailyHeartRateVariabilityImporter
import kenny.fitbit.importer.heartrate.HeartRateImporterImpl
import kenny.fitbit.importer.heartrate.HeartRateVariabilityDetailsImporter
import kenny.fitbit.importer.heartrate.RestingHeartRateImporter
import kenny.fitbit.importer.profile.AccountImporter
import kenny.fitbit.importer.sleep.*
import kenny.fitbit.importer.steps.StepsImporter
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.data.rest.RepositoryRestMvcAutoConfiguration
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.autoconfigure.graphql.GraphQlAutoConfiguration
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.stereotype.Component

@SpringBootApplication(
    scanBasePackages = ["kenny.fitbit.importer"],
    exclude = [
        GraphQlAutoConfiguration::class,
        WebMvcAutoConfiguration::class,
        DispatcherServletAutoConfiguration::class,
        ServletWebServerFactoryAutoConfiguration::class,
        ErrorMvcAutoConfiguration::class,
        RepositoryRestMvcAutoConfiguration::class
    ]
)
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
    val accountImporter: AccountImporter
) : ApplicationRunner {

    override fun run(args: ApplicationArguments) {
        val all = args.containsOption("all")
        fun hasOption(option: String) = all || args.containsOption(option)

        if (hasOption("heartrate")) {
            println("Importing heart rate data...")
            val count = heartRateImporter.import()
            println("Imported $count heart rate files")
        }

        if (hasOption("steps")) {
            println("Importing steps data...")
            val count = stepsImporter.import()
            println("Imported $count steps files")
        }

        if (hasOption("calories")) {
            println("Importing calories data...")
            val count = caloriesImporter.import()
            println("Imported $count calories files")
        }

        if (hasOption("distance")) {
            println("Importing distance data...")
            val count = distanceImporter.import()
            println("Imported $count distance files")
        }

        if (hasOption("restingheartrate")) {
            println("Importing resting heart rate data...")
            val count = restingHeartRateImporter.import()
            println("Imported $count resting heart rate files")
        }

        if (hasOption("exercise")) {
            println("Importing exercise data...")
            val count = exerciseImporter.import()
            println("Imported $count exercise files")
        }

        if (hasOption("timeinzone")) {
            println("Importing timeinzone data...")
            val count = timeInHeartRateZonesImporter.import()
            println("Imported $count timeinzone files")
        }

        if (hasOption("activityminutes")) {
            println("Importing activity minutes data...")
            val count = activityMinutesImporter.import()
            println("Imported $count activity minutes files")
        }

        if (hasOption("activezoneminutes")) {
            println("Importing active zone minutes data...")
            val count = activeZoneMinutesImporter.import()
            println("Imported $count active zone minutes files")
        }

        if (hasOption("vo2max")) {
            println("Importing demographic VO2 max data...")
            val count = demographicVO2MaxImporter.import()
            println("Imported $count demographic VO2 max files")
        }

        if (hasOption("runvo2max")) {
            println("Importing run VO2 max data...")
            val count = runVO2MaxImporter.import()
            println("Imported $count run VO2 max files")
        }

        if (hasOption("activitygoals")) {
            println("Importing activity goals data...")
            val count = activityGoalImporter.import()
            println("Imported $count activity goals files")
        }

        if (hasOption("sleep")) {
            println("Importing sleep data...")
            val count = sleepImporter.import()
            println("Imported $count sleep files")
        }

        if (hasOption("devicetemperature")) {
            println("Importing device temperature data...")
            val count = deviceTemperatureImporter.import()
            println("Imported $count device temperature files")
        }

        if (hasOption("respiratoryrate")) {
            println("Importing daily respiratory rate data...")
            val count = dailyRespiratoryRateImporter.import()
            println("Imported $count daily respiratory rate files")
        }

        if (hasOption("hrv")) {
            println("Importing daily heart rate variability data...")
            val count = dailyHeartRateVariabilityImporter.import()
            println("Imported $count daily heart rate variability files")
        }

        if (hasOption("hrvdetails")) {
            println("Importing heart rate variability details data...")
            val count = heartRateVariabilityDetailsImporter.import()
            println("Imported $count heart rate variability details files")
        }

        if (hasOption("minutespo2")) {
            println("Importing minute SpO2 data...")
            val count = minuteSpO2Importer.import()
            println("Imported $count minute SpO2 files")
        }

        if (hasOption("sleepscore")) {
            println("Importing sleep score data...")
            val count = sleepScoreImporter.import()
            println("Imported $count sleep score files")
        }

        if (hasOption("computedtemperature")) {
            println("Importing computed temperature data...")
            val count = computedTemperatureImporter.import()
            println("Imported $count computed temperature files")
        }

        if (hasOption("respiratoryratesummary")) {
            println("Importing respiratory rate summary data...")
            val count = respiratoryRateSummaryImporter.import()
            println("Imported $count respiratory rate summary files")
        }

        if (hasOption("dailyspo2")) {
            println("Importing daily SpO2 data...")
            val count = dailySpO2Importer.import()
            println("Imported $count daily SpO2 files")
        }

        if (hasOption("profile")) {
            println("Importing profile data...")
            val count = accountImporter.import()
            println("Imported $count profile files")
        }
    }
}
