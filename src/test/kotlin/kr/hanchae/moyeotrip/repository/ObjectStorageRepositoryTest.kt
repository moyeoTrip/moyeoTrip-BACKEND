package kr.hanchae.moyeotrip.repository

import io.awspring.cloud.s3.ObjectMetadata
import io.awspring.cloud.s3.S3Resource
import io.awspring.cloud.s3.S3Template
import kr.hanchae.moyeotrip.config.properties.StorageS3Properties
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.net.URI

class ObjectStorageRepositoryTest {
    private val properties =
        StorageS3Properties(
            endpoint = URI("https://s3.example.com"),
            region = "ap-northeast-2",
            accessKey = "access",
            secretKey = "secret",
            bucket = "moyeo-trip",
            cdnUrl = "https://cdn.example.com",
        )
    private val s3Template = mock(S3Template::class.java)
    private val repository = ObjectStorageRepository(properties, s3Template)

    @Test
    fun `일반 파일 업로드에도 immutable 1년 캐시 메타데이터를 설정한다`() {
        val stream = "image".byteInputStream()
        val uploaded = mock(S3Resource::class.java)
        `when`(
            s3Template.upload(
                eq("moyeo-trip"),
                eq("feed/image/test.jpg"),
                eq(stream),
                any(ObjectMetadata::class.java),
            ),
        ).thenReturn(uploaded)
        `when`(uploaded.filename).thenReturn("feed/image/test.jpg")

        repository.upload("feed/image/", "test.jpg", stream)

        val metadata = ArgumentCaptor.forClass(ObjectMetadata::class.java)
        verify(s3Template).upload(eq("moyeo-trip"), eq("feed/image/test.jpg"), eq(stream), metadata.capture())
        assertEquals("public, max-age=31536000, immutable", metadata.value.cacheControl)
    }
}
