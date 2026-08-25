package kr.hanchae.moyeotrip.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("client.weather")
data class WeatherApiProperties(
    val serviceKey: String = "",
    val baseUrl: String = "https://apis.data.go.kr",
    val primaryLocationName: String = "경상북도 포항시",
    val primaryGridX: Int = 102,
    val primaryGridY: Int = 94,
    val fallbackLocationName: String = "경상북도 안동시",
    val fallbackGridX: Int = 91,
    val fallbackGridY: Int = 106,
)
