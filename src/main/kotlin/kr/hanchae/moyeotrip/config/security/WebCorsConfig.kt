package kr.hanchae.moyeotrip.config.security

import kr.hanchae.moyeotrip.config.properties.WebCorsProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
class WebCorsConfig(
    private val properties: WebCorsProperties,
) {
    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration =
            CorsConfiguration().apply {
                allowedOrigins =
                    properties.allowedOrigins
                        .map(String::trim)
                        .filter(String::isNotBlank)
                        .distinct()
                allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                allowedHeaders = listOf("Authorization", "Content-Type", "Accept", "X-Trace-Id")
                exposedHeaders = listOf("X-Trace-Id")
                allowCredentials = false
                maxAge = properties.maxAgeSeconds.coerceAtLeast(0)
            }

        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/api/**", configuration)
        }
    }
}
