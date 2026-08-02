package kr.hanchae.moyeotrip.config

import kr.hanchae.moyeotrip.config.properties.StorageS3Properties
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import software.amazon.awssdk.regions.Region
import java.net.URI

class S3ConfigTest {
    @Test
    fun `커스텀 스토리지 설정으로 S3 클라이언트를 생성한다`() {
        val properties =
            StorageS3Properties(
                endpoint = URI.create("https://object-storage.example.com"),
                region = "ap-northeast-2",
                accessKey = "test-access-key",
                secretKey = "test-secret-key",
                bucket = "test-bucket",
                cdnUrl = "https://cdn.example.com",
            )
        val client = S3Config().s3Client(properties)

        try {
            val configuration = client.serviceClientConfiguration()
            assertEquals(properties.endpoint, configuration.endpointOverride().orElseThrow())
            assertEquals(Region.of(properties.region), configuration.region())
        } finally {
            client.close()
        }
    }
}
