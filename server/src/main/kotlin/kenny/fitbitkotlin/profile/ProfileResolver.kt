package kenny.fitbitkotlin.profile

import org.springframework.data.domain.PageRequest
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.stereotype.Controller

@Controller
class ProfileResolver(private val profileRepository: ProfileRepository) {

    @QueryMapping
    fun profiles(@Argument limit: Int, @Argument offset: Int): List<Profile> {
        val pageable = PageRequest.of(offset / limit, limit)
        return profileRepository.findAll(pageable).content
    }

    @QueryMapping
    fun profile(@Argument id: String): Profile? {
        return profileRepository.findById(id).orElse(null)
    }

    @SchemaMapping(typeName = "Profile")
    fun avatar(profile: Profile): String? {
        // Convert the avatar byte array to a Base64 encoded string for GraphQL
        return profile.avatar?.let { java.util.Base64.getEncoder().encodeToString(it) }
    }
}

