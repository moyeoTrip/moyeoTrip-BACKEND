package kr.hanchae.moyeotrip.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "apple.oauth2")
data class AppleOAuth2Properties(
    val clientId: String,
    val teamId: String,
    val keyId: String,
    val p8PrivateKey: String,
    val redirectUrl: String,
)
