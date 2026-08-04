package kr.hanchae.moyeotrip.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("kakao")
data class KakaoProperties(
    val appId: Long,
    val restApiKey: String,
    val clientSecret: String,
    val allowedRedirectUris: List<String>,
)
