package kr.hanchae.moyeotrip.client

import kr.hanchae.moyeotrip.exception.BaseException
import kr.hanchae.moyeotrip.exception.ErrorCode
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.ai.image.Image
import org.springframework.ai.image.ImageGeneration
import org.springframework.ai.image.ImageResponse
import java.util.Base64

class ProfileImageGenerationClientTest {
    @Test
    fun `Spring AI 이미지 응답을 바이너리로 변환한다`() {
        val expected = byteArrayOf(1, 2, 3, 4)
        val encoded = Base64.getEncoder().encodeToString(expected)
        val client =
            ProfileImageGenerationClient { ImageResponse(listOf(ImageGeneration(Image(null, encoded)))) }

        assertArrayEquals(expected, client.generate("profile prompt"))
    }

    @Test
    fun `이미지 결과가 없으면 프로필 이미지 생성 실패로 변환한다`() {
        val client = ProfileImageGenerationClient { ImageResponse(emptyList()) }

        val exception = assertThrows(BaseException::class.java) { client.generate("profile prompt") }

        assertEquals(ErrorCode.PROFILE_IMAGE_GENERATION_FAILED, exception.errorCode)
    }
}
