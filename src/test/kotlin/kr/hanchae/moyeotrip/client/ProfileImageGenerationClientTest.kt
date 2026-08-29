package kr.hanchae.moyeotrip.client

import com.openai.client.OpenAIClient
import com.openai.models.images.Image
import com.openai.models.images.ImageEditParams
import com.openai.models.images.ImagesResponse
import com.openai.services.blocking.ImageService
import kr.hanchae.moyeotrip.exception.BaseException
import kr.hanchae.moyeotrip.exception.ErrorCode
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.ai.model.openai.autoconfigure.OpenAiImageProperties
import org.springframework.core.io.ByteArrayResource
import java.util.Base64
import java.util.Optional

class ProfileImageGenerationClientTest {
    @Test
    fun `OpenAI 이미지 편집 응답을 바이너리로 변환한다`() {
        val expected = byteArrayOf(1, 2, 3, 4)
        val encoded = Base64.getEncoder().encodeToString(expected)
        val fixture = imageEditFixture(Optional.of(encoded))

        assertArrayEquals(expected, fixture.client.generate("profile prompt"))
        val requestCaptor = ArgumentCaptor.forClass(ImageEditParams::class.java)
        verify(fixture.imageService).edit(captureImageEditParams(requestCaptor))
        with(requestCaptor.value) {
            assertEquals("profile prompt", prompt())
            assertArrayEquals(fixture.referenceImage, image().asInputStream().readBytes())
            assertEquals(ImageEditParams.InputFidelity.HIGH, inputFidelity().orElseThrow())
            assertEquals(ImageEditParams.Quality.LOW, quality().orElseThrow())
            assertEquals("gpt-image-2", model().orElseThrow().asString())
        }
    }

    @Test
    fun `이미지 편집 결과가 없으면 프로필 이미지 생성 실패로 변환한다`() {
        val fixture = imageEditFixture(Optional.empty())

        val exception = assertThrows(BaseException::class.java) { fixture.client.generate("profile prompt") }

        assertEquals(ErrorCode.PROFILE_IMAGE_GENERATION_FAILED, exception.errorCode)
    }

    private fun imageEditFixture(encodedImage: Optional<String>): Fixture {
        val referenceImage = byteArrayOf(9, 8, 7)
        val openAIClient = mock(OpenAIClient::class.java)
        val imageService = mock(ImageService::class.java)
        val response = mock(ImagesResponse::class.java)
        val image = mock(Image::class.java)
        `when`(openAIClient.images()).thenReturn(imageService)
        `when`(imageService.edit(anyImageEditParams())).thenReturn(response)
        `when`(response.data()).thenReturn(Optional.of(listOf(image)))
        `when`(image.b64Json()).thenReturn(encodedImage)
        val properties =
            OpenAiImageProperties().apply {
                model = "gpt-image-2"
                quality = "low"
                size = "1024x1024"
            }
        return Fixture(
            ProfileImageGenerationClient(
                openAIClient = openAIClient,
                imageProperties = properties,
                styleReference = ByteArrayResource(referenceImage),
            ),
            imageService,
            referenceImage,
        )
    }

    private data class Fixture(
        val client: ProfileImageGenerationClient,
        val imageService: ImageService,
        val referenceImage: ByteArray,
    )

    private fun anyImageEditParams(): ImageEditParams =
        any(ImageEditParams::class.java)
            ?: testImageEditParams()

    private fun captureImageEditParams(captor: ArgumentCaptor<ImageEditParams>): ImageEditParams = captor.capture() ?: testImageEditParams()

    private fun testImageEditParams(): ImageEditParams =
        ImageEditParams
            .builder()
            .image(byteArrayOf(0))
            .prompt("test matcher")
            .build()
}
