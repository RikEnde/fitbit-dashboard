package kenny.fitbit

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@SpringBootApplication
@EntityScan(basePackages = ["kenny.fitbit"])
@EnableJpaRepositories(basePackages = ["kenny.fitbit"])
class TestConfig
