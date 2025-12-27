package kenny.fitbitkotlin

import com.fasterxml.jackson.databind.JsonNode
import kenny.fitbitkotlin.profile.AccountImporter
import kenny.fitbitkotlin.profile.AccountImporterImpl
import kenny.fitbitkotlin.profile.Profile
import kenny.fitbitkotlin.profile.ProfileRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.io.File

@SpringBootTest
class AccountImporterImplTest {

    @Autowired
    private lateinit var accountImporter: AccountImporterImpl

    @Autowired
    private lateinit var profileRepository: ProfileRepository

    @Test
    fun `test file pattern matches Profile csv`() {
        val pattern = accountImporter.filePattern().toRegex()
        assertTrue(pattern.matches("Profile.csv"), "Pattern should match Profile.csv")
        assertFalse(pattern.matches("Profile.csv.imported"), "Pattern should not match imported files")
        assertFalse(pattern.matches("OtherFile.csv"), "Pattern should not match other CSV files")
    }

    @Test
    fun `test directory returns correct path`() {
        assertEquals("Personal & Account", accountImporter.directory())
    }

    @Test
    fun `test avatar path returns correct path`() {
        assertEquals("Personal & Account/Media/Avatar Photo.jpg", accountImporter.avatarpath())
    }

    @Test
    fun `test import with test data`() {
        // Use test resources directory
        val testDataDir = javaClass.classLoader.getResource("testdata")?.path
            ?: throw IllegalStateException("Test data directory not found")

        // Create a test-specific importer that uses test data directory
        val testImporter = object : AccountImporter {
            override val dataDir: String = testDataDir
            override fun parseAndSaveEntity(jsonItem: JsonNode) {
                // Not used for this test
            }
        }

        // Verify test files exist
        val profileDir = File(testDataDir, testImporter.directory())
        assertTrue(profileDir.exists(), "Test profile directory should exist at ${profileDir.absolutePath}")

        val files = testImporter.files()
        assertTrue(files.isNotEmpty(), "Should find test Profile.csv file")
        assertEquals("Profile.csv", files.first().name)
    }

    @Test
    fun `test CSV parsing logic`() {
        val testDataDir = javaClass.classLoader.getResource("testdata")?.path
            ?: throw IllegalStateException("Test data directory not found")

        val csvFile = File(testDataDir, "Personal & Account/Profile.csv")
        assertTrue(csvFile.exists(), "Test CSV file should exist")

        val lines = csvFile.readLines()
        assertEquals(2, lines.size, "CSV should have header and one data row")

        val header = lines[0].split(",")
        val data = lines[1].split(",")
        val rowData = header.zip(data).toMap()

        assertEquals("TEST123", rowData["id"])
        assertEquals("Test User", rowData["full_name"])
        assertEquals("Test", rowData["first_name"])
        assertEquals("User", rowData["last_name"])
        assertEquals("test@example.com", rowData["email_address"])
        assertEquals("1990-01-15", rowData["date_of_birth"])
        assertEquals("false", rowData["child"])
        assertEquals("US", rowData["country"])
        assertEquals("175.0", rowData["height"])
        assertEquals("70.0", rowData["weight"])
    }

    @Test
    fun `test Profile entity creation from CSV data`() {
        val profile = Profile(
            id = "TEST123",
            fullName = "Test User",
            firstName = "Test",
            lastName = "User",
            displayNameSetting = "name",
            displayName = "Test U.",
            username = "testuser",
            emailAddress = "test@example.com",
            dateOfBirth = "1990-01-15",
            child = false,
            country = "US",
            state = "CA",
            city = "San Francisco",
            timezone = "America/Los_Angeles",
            locale = "en_US",
            memberSince = "2020-01-01",
            aboutMe = "Test about me",
            startOfWeek = "SUNDAY",
            sleepTracking = "Normal",
            timeDisplayFormat = "24hour",
            gender = "MALE",
            height = 175.0,
            weight = 70.0,
            strideLengthWalking = 70.0,
            strideLengthRunning = 120.0,
            weightUnit = "METRIC",
            distanceUnit = "METRIC",
            heightUnit = "METRIC",
            waterUnit = "METRIC",
            glucoseUnit = "METRIC",
            swimUnit = "METRIC",
            avatar = byteArrayOf(1, 2, 3, 4)
        )

        assertEquals("TEST123", profile.id)
        assertEquals("Test User", profile.fullName)
        assertEquals("test@example.com", profile.emailAddress)
        assertEquals(175.0, profile.height)
        assertEquals(70.0, profile.weight)
        assertNotNull(profile.avatar)
        assertEquals(4, profile.avatar?.size)
    }

    @Test
    fun `test Profile repository save and retrieve`() {
        // Clean up any existing test data
        profileRepository.findById("TEST_REPO").ifPresent { profileRepository.delete(it) }

        val profile = Profile(
            id = "TEST_REPO",
            fullName = "Repository Test",
            firstName = "Repo",
            lastName = "Test",
            displayNameSetting = "name",
            displayName = "Repo T.",
            username = null,
            emailAddress = "repo@test.com",
            dateOfBirth = "2000-01-01",
            child = false,
            country = "US",
            state = null,
            city = null,
            timezone = "UTC",
            locale = "en_US",
            memberSince = "2024-01-01",
            aboutMe = null,
            startOfWeek = "MONDAY",
            sleepTracking = "Normal",
            timeDisplayFormat = "12hour",
            gender = "FEMALE",
            height = 165.0,
            weight = 60.0,
            strideLengthWalking = 65.0,
            strideLengthRunning = 110.0,
            weightUnit = "METRIC",
            distanceUnit = "METRIC",
            heightUnit = "METRIC",
            waterUnit = "METRIC",
            glucoseUnit = "METRIC",
            swimUnit = "METRIC",
            avatar = null
        )

        val saved = profileRepository.save(profile)
        assertEquals("TEST_REPO", saved.id)

        val retrieved = profileRepository.findById("TEST_REPO")
        assertTrue(retrieved.isPresent)
        assertEquals("Repository Test", retrieved.get().fullName)
        assertEquals("repo@test.com", retrieved.get().emailAddress)

        // Clean up
        profileRepository.delete(saved)
    }

    @Test
    @Disabled("Only run manually with actual data files")
    fun `test full import with real data`() {
        profileRepository.deleteAll()

        val profileCount = accountImporter.import()

        println("Found $profileCount profile records")
        assertTrue(profileCount > 0, "Expected at least one profile to be imported")

        val profiles = profileRepository.findAll()
        assertTrue(profiles.isNotEmpty(), "Expected at least one profile in the database")

        val profile = profiles.first()
        assertNotNull(profile.id)
        assertNotNull(profile.fullName)

        println("Profile ID: ${profile.id}")
        println("Profile Name: ${profile.fullName}")
        println("Profile Email: ${profile.emailAddress}")
        println("Profile Avatar Size: ${profile.avatar?.size ?: 0} bytes")
    }
}
