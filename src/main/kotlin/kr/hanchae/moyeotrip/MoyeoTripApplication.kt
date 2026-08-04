package kr.hanchae.moyeotrip

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@ConfigurationPropertiesScan
@Configuration
@EnableJpaAuditing
@EnableScheduling
class MoyeoTripApplication

fun main(args: Array<String>) {
    runApplication<MoyeoTripApplication>(*args)
}
