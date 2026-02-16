package kenny.fitbit

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class FitbitServerApplication

fun main(args: Array<String>) {
    runApplication<FitbitServerApplication>(*args)
}
