package kenny.fitbit.auth

import org.springframework.http.ResponseEntity
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

data class RegistrationRequest(val username: String, val password: String)

@RestController
class RegistrationController(
    private val userCredentialsRepository: UserCredentialsRepository,
    private val passwordEncoder: PasswordEncoder
) {

    @PostMapping("/api/register")
    fun register(@RequestBody request: RegistrationRequest): ResponseEntity<Map<String, String>> {
        val username = request.username.trim()

        if (username.length < 3 || username.length > 50) {
            return ResponseEntity.badRequest().body(mapOf("error" to "Username must be between 3 and 50 characters"))
        }
        if (!username.matches(Regex("^[a-zA-Z0-9_-]+$"))) {
            return ResponseEntity.badRequest().body(mapOf("error" to "Username may only contain letters, numbers, hyphens, and underscores"))
        }
        if (request.password.length < 8) {
            return ResponseEntity.badRequest().body(mapOf("error" to "Password must be at least 8 characters"))
        }
        if (userCredentialsRepository.findByUsername(username) != null) {
            return ResponseEntity.badRequest().body(mapOf("error" to "Username is already taken"))
        }

        userCredentialsRepository.save(
            UserCredentials(
                username = username,
                hash = passwordEncoder.encode(request.password)
            )
        )

        return ResponseEntity.ok(mapOf("message" to "Account created"))
    }
}
