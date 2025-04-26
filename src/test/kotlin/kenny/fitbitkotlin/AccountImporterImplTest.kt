package kenny.fitbitkotlin

import kenny.fitbitkotlin.profile.AccountImporterImpl
import kenny.fitbitkotlin.profile.ProfileRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.nio.file.Files
import java.nio.file.Paths

@SpringBootTest
class AccountImporterImplTest {

    @Autowired
    private lateinit var accountImporter: AccountImporterImpl

    @Autowired
    private lateinit var profileRepository: ProfileRepository

    @Test
    @Disabled("Only run manually")
    fun `test import returns profile data`() {
        // Delete any existing profiles to ensure a clean test
        profileRepository.deleteAll()
        
        // Call the import method
        val profileCount = accountImporter.import()

        // Verify that profiles were imported
        println("Found $profileCount profile records")
        assertTrue(profileCount > 0, "Expected at least one profile to be imported")

        // Verify that the profiles are in the database
        val profiles = profileRepository.findAll()
        assertTrue(profiles.isNotEmpty(), "Expected at least one profile in the database")

        // Verify that the first profile has the expected data
        val profile = profiles.first()
        assertNotNull(profile.id, "Profile ID should not be null")
        assertNotNull(profile.fullName, "Profile full name should not be null")
        assertNotNull(profile.firstName, "Profile first name should not be null")
        assertNotNull(profile.lastName, "Profile last name should not be null")
        
        // Verify that the avatar was imported
        assertNotNull(profile.avatar, "Profile avatar should not be null")
        assertTrue(profile.avatar!!.isNotEmpty(), "Profile avatar should not be empty")
        
        // Print some profile data for manual verification
        println("Profile ID: ${profile.id}")
        println("Profile Name: ${profile.fullName}")
        println("Profile Email: ${profile.emailAddress}")
        println("Profile Avatar Size: ${profile.avatar?.size ?: 0} bytes")

        // Call import again to simulate duplicate imports
        println("Calling import again to test for duplicate key handling")
        val profileCount2 = accountImporter.import()
        println("Found $profileCount2 profile records on second import")
    }

    @Test
    fun `test avatar file is correctly loaded`() {
        // Verify that the avatar file exists
        val avatarPath = Paths.get(accountImporter.dataDir, accountImporter.avatarpath())
        assertTrue(Files.exists(avatarPath), "Avatar file should exist at $avatarPath")
        
        // Verify that the avatar file can be read
        val avatarBytes = Files.readAllBytes(avatarPath)
        assertTrue(avatarBytes.isNotEmpty(), "Avatar file should not be empty")
        
        // Print the avatar file size for manual verification
        println("Avatar file size: ${avatarBytes.size} bytes")
    }
}