package kenny.fitbit

import kenny.fitbit.profile.Profile
import kenny.fitbit.profile.ProfileRepository
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthenticatedProfileService(private val profileRepository: ProfileRepository) {

    @Transactional(readOnly = true)
    fun getProfile(): Profile {
        val username = SecurityContextHolder.getContext().authentication.name
        return profileRepository.findByUsername(username)
            ?: throw IllegalStateException("No profile found for authenticated user: $username")
    }

    @Transactional(readOnly = true)
    fun getProfileOrNull(): Profile? {
        val username = SecurityContextHolder.getContext().authentication.name
        return profileRepository.findByUsername(username)
    }
}
