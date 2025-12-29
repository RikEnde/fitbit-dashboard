package kenny.fitbit

import graphql.scalars.ExtendedScalars
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.graphql.execution.RuntimeWiringConfigurer
import org.springframework.web.servlet.config.annotation.EnableWebMvc
import java.time.LocalDateTime
import java.time.OffsetDateTime

data class DateRange(val from: OffsetDateTime, val to: OffsetDateTime) {
    // Convert to LocalDateTime for database queries (data is stored in local time)
    val fromLocal: LocalDateTime get() = from.toLocalDateTime()
    val toLocal: LocalDateTime get() = to.toLocalDateTime()
}

@Configuration
@EnableWebMvc
class GraphQLConfig {

    @Bean
    fun runtimeWiringConfigurer(): RuntimeWiringConfigurer =
        RuntimeWiringConfigurer { builder ->
            builder.scalar(ExtendedScalars.DateTime)
        }
}
