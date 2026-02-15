package kenny.fitbit

import kenny.fitbit.auth.UserCredentials
import kenny.fitbit.auth.UserCredentialsRepository
import kenny.fitbit.profile.Profile
import kenny.fitbit.profile.ProfileRepository
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.security.crypto.password.PasswordEncoder

@TestConfiguration
class TestConfig {

    @Bean
    fun testDataInitializer(
        profileRepository: ProfileRepository,
        userCredentialsRepository: UserCredentialsRepository,
        passwordEncoder: PasswordEncoder
    ) = ApplicationRunner {
        val profile = profileRepository.save(testProfile())
        userCredentialsRepository.save(
            UserCredentials(
                profile = profile,
                hash = passwordEncoder.encode("testpass")
            )
        )
    }

    companion object {
        fun testProfile() = Profile(
            id = "TEST123",
            fullName = "Test User",
            firstName = "Test",
            lastName = "User",
            displayNameSetting = "name",
            displayName = "Test",
            username = "testuser",
            emailAddress = "test@example.com",
            dateOfBirth = "1990-01-01",
            child = false,
            country = "US",
            state = null,
            city = null,
            timezone = "America/New_York",
            locale = "en_US",
            memberSince = "2020-01-01",
            aboutMe = null,
            startOfWeek = "MONDAY",
            sleepTracking = "Normal",
            timeDisplayFormat = "24hour",
            gender = "NA",
            height = 170.0,
            weight = 70.0,
            strideLengthWalking = 70.0,
            strideLengthRunning = 90.0,
            weightUnit = "METRIC",
            distanceUnit = "METRIC",
            heightUnit = "METRIC",
            waterUnit = "METRIC",
            glucoseUnit = "METRIC",
            swimUnit = "METRIC"
        )
    }
}
