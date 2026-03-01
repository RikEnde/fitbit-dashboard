package kenny.fitbit.auth

import kenny.fitbit.TestConfig
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestConfig::class)
class RegistrationControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `register creates account successfully`() {
        mockMvc.perform(
            post("/api/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"username":"newuser","password":"password123"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.message").value("Account created"))
    }

    @Test
    fun `register rejects duplicate username`() {
        mockMvc.perform(
            post("/api/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"username":"testuser","password":"password123"}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("Registration failed"))
    }

    @Test
    fun `register rejects short username`() {
        mockMvc.perform(
            post("/api/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"username":"ab","password":"password123"}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("Username must be between 3 and 50 characters"))
    }

    @Test
    fun `register rejects invalid username characters`() {
        mockMvc.perform(
            post("/api/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"username":"bad user!","password":"password123"}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("Username may only contain letters, numbers, hyphens, and underscores"))
    }

    @Test
    fun `register rejects short password`() {
        mockMvc.perform(
            post("/api/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"username":"validuser","password":"short"}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("Password must be at least 8 characters"))
    }

    @Test
    fun `can login after registration`() {
        // Register
        mockMvc.perform(
            post("/api/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"username":"logintest","password":"password123"}""")
        ).andExpect(status().isOk)

        // Login with new credentials
        mockMvc.perform(
            post("/graphql")
                .with(httpBasic("logintest", "password123"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"query":"{ __typename }"}""")
        ).andExpect(status().isOk)
    }

    @Test
    fun `register endpoint does not require authentication`() {
        mockMvc.perform(
            post("/api/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"username":"noauthuser","password":"password123"}""")
        ).andExpect(status().isOk)
    }
}
