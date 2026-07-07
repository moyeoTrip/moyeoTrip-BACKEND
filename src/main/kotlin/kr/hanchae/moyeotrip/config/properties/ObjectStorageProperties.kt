package kr.hanchae.moyeotrip.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "spring.cloud.aws.s3")
data class ObjectStorageProperties(
    val bucket: String,
    val cdnUrl: String,
)
