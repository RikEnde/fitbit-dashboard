package kenny.fitbit.profile

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDate

/**
 * Profile importer with custom implementation.
 * Reads a single CSV file and an avatar image - doesn't fit the standard CsvImporter pattern.
 */
@Component
class AccountImporterImpl(
    private val profileRepository: ProfileRepository
) : AccountImporter {

    override var dataDir: String = "../data"
    override var userDir: String? = null
    override var profile: Profile? = null
    override var maxDate: LocalDate? = null

    val maxConcurrentFiles: Int
        get() = 10

    override fun import(): Int {
        val files = files()
        val size = files.size
        val semaphore = Semaphore(maxConcurrentFiles)

        runBlocking(Dispatchers.IO) {
            val jobs = files.mapIndexed { index, file ->
                launch(Dispatchers.IO) {
                    semaphore.acquire()
                    try {
                        importFile(index, size, file)
                        file.renameTo(File(file.absolutePath + ".imported"))
                    } finally {
                        semaphore.release()
                    }
                }
            }
            jobs.joinAll()
        }

        println("Completed processing ${files.size} files")
        return size
    }

    @Transactional(propagation = Propagation.REQUIRED)
    suspend fun importFile(index: Int, size: Int, file: File) {
        println("Processing file ${index + 1} of $size (%.4f%%".format(100.0 * index / size))
        println("Parsing $file")
        try {
            // Read the CSV file
            val lines = file.readLines()
            if (lines.size < 2) {
                println("CSV file has no data rows")
                return
            }

            val header = lines[0].split(",")
            val data = lines[1].split(",")

            // Create a map of column name to value
            val rowData = header.zip(data).toMap()

            // Read the avatar image
            val avatarBasePath = if (userDir != null) Paths.get(dataDir, userDir, avatarpath()) else Paths.get(dataDir, avatarpath())
            val avatarBytes = if (Files.exists(avatarBasePath)) {
                Files.readAllBytes(avatarBasePath)
            } else {
                println("Avatar image not found at $avatarBasePath")
                null
            }

            // Create and save the profile entity, using directory name as username
            val importedProfile = Profile(
                id = rowData["id"] ?: "",
                fullName = rowData["full_name"] ?: "",
                firstName = rowData["first_name"] ?: "",
                lastName = rowData["last_name"] ?: "",
                displayNameSetting = rowData["display_name_setting"] ?: "",
                displayName = rowData["display_name"] ?: "",
                username = userDir ?: rowData["username"],
                emailAddress = rowData["email_address"] ?: "",
                dateOfBirth = rowData["date_of_birth"] ?: "",
                child = rowData["child"]?.toBoolean() ?: false,
                country = rowData["country"] ?: "",
                state = rowData["state"],
                city = rowData["city"],
                timezone = rowData["timezone"] ?: "",
                locale = rowData["locale"] ?: "",
                memberSince = rowData["member_since"] ?: "",
                aboutMe = rowData["about_me"],
                startOfWeek = rowData["start_of_week"] ?: "",
                sleepTracking = rowData["sleep_tracking"] ?: "",
                timeDisplayFormat = rowData["time_display_format"] ?: "",
                gender = rowData["gender"] ?: "",
                height = rowData["height"]?.toDoubleOrNull() ?: 0.0,
                weight = rowData["weight"]?.toDoubleOrNull() ?: 0.0,
                strideLengthWalking = rowData["stride_length_walking"]?.toDoubleOrNull() ?: 0.0,
                strideLengthRunning = rowData["stride_length_running"]?.toDoubleOrNull() ?: 0.0,
                weightUnit = rowData["weight_unit"] ?: "",
                distanceUnit = rowData["distance_unit"] ?: "",
                heightUnit = rowData["height_unit"] ?: "",
                waterUnit = rowData["water_unit"] ?: "",
                glucoseUnit = rowData["glucose_unit"] ?: "",
                swimUnit = rowData["swim_unit"] ?: "",
                avatar = avatarBytes
            )

            profileRepository.save(importedProfile)
            profile = importedProfile
            println("Saved profile with ID: ${importedProfile.id}, username: ${importedProfile.username}")

        } catch (e: Exception) {
            println("Error parsing file ${file.name}: ${e.message}")
            e.printStackTrace()
        }
    }
}
