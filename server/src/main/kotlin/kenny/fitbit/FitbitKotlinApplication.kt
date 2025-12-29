package kenny.fitbit

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class FitbitKotlinApplication

fun main(args: Array<String>) {
    runApplication<FitbitKotlinApplication>(*args)
}
