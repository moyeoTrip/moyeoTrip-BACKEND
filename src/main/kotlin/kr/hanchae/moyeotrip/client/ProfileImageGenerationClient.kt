package kr.hanchae.moyeotrip.client

import com.openai.client.OpenAIClient
import com.openai.models.images.ImageEditParams
import kr.hanchae.moyeotrip.config.PROFILE_IMAGE_OPEN_AI_CLIENT
import kr.hanchae.moyeotrip.exception.BaseException
import kr.hanchae.moyeotrip.exception.ErrorCode
import org.slf4j.LoggerFactory
import org.springframework.ai.model.openai.autoconfigure.OpenAiImageProperties
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.stereotype.Component
import java.util.Base64

@Component
class ProfileImageGenerationClient(
    @Qualifier(PROFILE_IMAGE_OPEN_AI_CLIENT)
    private val openAIClient: OpenAIClient,
    private val imageProperties: OpenAiImageProperties,
    @Value("classpath:/profile-image/moyeotrip-character-style-reference.png")
    styleReference: Resource,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val styleReferenceBytes = styleReference.inputStream.use { it.readBytes() }

    fun generate(prompt: String): ByteArray =
        try {
            val request =
                ImageEditParams
                    .builder()
                    .image(styleReferenceBytes)
                    .prompt(prompt)
                    .model(imageProperties.model ?: DEFAULT_MODEL)
                    .inputFidelity(ImageEditParams.InputFidelity.HIGH)
                    .n(1)
                    .outputFormat(ImageEditParams.OutputFormat.PNG)
                    .quality(ImageEditParams.Quality.of(imageProperties.quality ?: DEFAULT_QUALITY))
                    .size(imageProperties.size ?: DEFAULT_SIZE)
                    .build()
            val encodedImage =
                openAIClient
                    .images()
                    .edit(request)
                    .data()
                    .orElseThrow { IllegalStateException("이미지 편집 결과가 비어 있습니다.") }
                    .firstOrNull()
                    ?.b64Json()
                    ?.orElseThrow { IllegalStateException("이미지 편집 데이터가 비어 있습니다.") }
                    ?: throw IllegalStateException("이미지 편집 결과가 비어 있습니다.")
            Base64.getDecoder().decode(encodedImage)
        } catch (exception: Exception) {
            log.warn("AI 프로필 이미지 생성에 실패했습니다.", exception)
            throw BaseException(ErrorCode.PROFILE_IMAGE_GENERATION_FAILED, ErrorCode.PROFILE_IMAGE_GENERATION_FAILED.errorMessage)
        }

    companion object {
        private const val DEFAULT_MODEL = "gpt-image-2"
        private const val DEFAULT_QUALITY = "low"
        private const val DEFAULT_SIZE = "1024x1024"
    }
}
