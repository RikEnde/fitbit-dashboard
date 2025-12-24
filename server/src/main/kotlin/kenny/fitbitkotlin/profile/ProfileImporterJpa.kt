package kenny.fitbitkotlin.profile

import com.fasterxml.jackson.databind.JsonNode
import kotlinx.coroutines.*
import org.springframework.stereotype.Component
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

@Component
class AccountImporterImpl(private val profileRepository: ProfileRepository) : AccountImporter {
    override fun parseAndSaveEntity(jsonItem: JsonNode) {
        // This method is not used for CSV imports
    }

    override fun import(): Int {
        val files = files()
        val size = files.size

        runBlocking(Dispatchers.IO) {
            val jobs = files.mapIndexed { index, file ->
                launch(Dispatchers.IO) {
                    importFile(index, size, file)
                    file.renameTo(File(file.absolutePath + ".imported"))
                }
            }
            jobs.joinAll()
        }

        println("Completed processing ${files.size} files")
        return size
    }

    suspend fun importFile(index: Int, size: Int, file: File) {
        println("Processing file ${index + 1} of $size (%.4f".format(100.0 * index / size))
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
            val avatarPath = Paths.get(dataDir, avatarpath())
            val avatarBytes = if (Files.exists(avatarPath)) {
                Files.readAllBytes(avatarPath)
            } else {
                println("Avatar image not found at $avatarPath")
                null
            }

            // Create and save the profile entity
            val profile = Profile(
                id = rowData["id"] ?: "",
                fullName = rowData["full_name"] ?: "",
                firstName = rowData["first_name"] ?: "",
                lastName = rowData["last_name"] ?: "",
                displayNameSetting = rowData["display_name_setting"] ?: "",
                displayName = rowData["display_name"] ?: "",
                username = rowData["username"],
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

            profileRepository.save(profile)
            println("Saved profile with ID: ${profile.id}")

        } catch (e: Exception) {
            println("Error parsing file ${file.name}: ${e.message}")
            e.printStackTrace()
        }
    }
}