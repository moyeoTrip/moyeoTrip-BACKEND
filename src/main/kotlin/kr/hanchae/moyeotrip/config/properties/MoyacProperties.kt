package kr.hanchae.moyeotrip.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import java.net.URI

@ConfigurationProperties(prefix = "moyeotrip.storage.s3")
data class StorageS3Properties(
    val endpoint: URI,
    val region: String,
    val accessKey: String,
    val secretKey: String,
    val bucket: String,
)
