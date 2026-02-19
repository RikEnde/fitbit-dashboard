package kenny.fitbit.profile

import kenny.fitbit.AuthenticatedProfileService
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.stereotype.Controller

@Controller
class ProfileResolver(private val authService: AuthenticatedProfileService) {

    @QueryMapping
    fun profile(): Profile? {
        return authService.getProfileOrNull()
    }

    @SchemaMapping(typeName = "Profile")
    fun avatar(profile: Profile): String? {
        return profile.avatar?.let { java.util.Base64.getEncoder().encodeToString(it) }
    }
}
