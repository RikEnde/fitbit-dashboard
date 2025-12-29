package kenny.fitbit

import graphql.scalars.ExtendedScalars
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.graphql.execution.RuntimeWiringConfigurer
import org.springframework.web.servlet.config.annotation.EnableWebMvc
import java.time.LocalDateTime

data class DateRange(val from: LocalDateTime, val to: LocalDateTime)

@Configuration
@EnableWebMvc
class GraphQLConfig {

    @Bean
    fun runtimeWiringConfigurer(): RuntimeWiringConfigurer =
        RuntimeWiringConfigurer { builder ->
            builder.scalar(ExtendedScalars.DateTime)
        }
}
