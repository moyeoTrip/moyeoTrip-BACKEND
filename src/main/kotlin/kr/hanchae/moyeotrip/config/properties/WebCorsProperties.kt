package kr.hanchae.moyeotrip.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("moyeotrip.web.cors")
data class WebCorsProperties(
    val allowedOrigins: List<String> = DEFAULT_ALLOWED_ORIGINS,
    val maxAgeSeconds: Long = 3600,
) {
    companion object {
        val DEFAULT_ALLOWED_ORIGINS =
            listOf(
                "http://localhost:4173",
                "http://127.0.0.1:4173",
                "http://localhost:5173",
                "http://127.0.0.1:5173",
                "https://moyeo-trip.jayden-bin.cc",
            )
    }
}
