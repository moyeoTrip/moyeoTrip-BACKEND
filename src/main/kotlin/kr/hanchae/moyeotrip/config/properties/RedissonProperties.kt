package kr.hanchae.moyeotrip.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "spring.data.redis")
data class RedissonProperties(
    val host: String,
    val port: Int,
    val password: String,
    val database: Int,
)
