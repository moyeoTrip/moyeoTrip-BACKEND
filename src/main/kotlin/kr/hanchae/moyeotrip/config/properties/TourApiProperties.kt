package kr.hanchae.moyeotrip.config.properties

import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "client.tour")
data class TourApiProperties(
    @field:JsonProperty("tour-api-key")
    val tourApiKey: String,
    val syncOnStartup: Boolean = false,
)
