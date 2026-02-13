package kenny.fitbit

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `graphql endpoint returns 401 without credentials`() {
        mockMvc.perform(
            post("/graphql")
                .contentType("application/json")
                .content("""{"query":"{ __typename }"}""")
        ).andExpect(status().isUnauthorized)
    }

    @Test
    fun `graphql endpoint returns 200 with valid credentials`() {
        mockMvc.perform(
            post("/graphql")
                .with(httpBasic("testuser", "testpass"))
                .contentType("application/json")
                .content("""{"query":"{ __typename }"}""")
        ).andExpect(status().isOk)
    }

    @Test
    fun `graphql endpoint returns 401 with wrong credentials`() {
        mockMvc.perform(
            post("/graphql")
                .with(httpBasic("testuser", "wrongpassword"))
                .contentType("application/json")
                .content("""{"query":"{ __typename }"}""")
        ).andExpect(status().isUnauthorized)
    }

    @Test
    fun `rest api returns 401 without credentials`() {
        mockMvc.perform(get("/api"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `rest api returns 200 with valid credentials`() {
        mockMvc.perform(
            get("/api").with(httpBasic("testuser", "testpass"))
        ).andExpect(status().isOk)
    }

    @Test
    fun `graphiql returns 401 without credentials`() {
        mockMvc.perform(get("/graphiql"))
            .andExpect(status().isUnauthorized)
    }
}
