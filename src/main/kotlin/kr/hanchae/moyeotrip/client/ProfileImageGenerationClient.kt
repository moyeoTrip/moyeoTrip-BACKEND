package kr.hanchae.moyeotrip.client

import kr.hanchae.moyeotrip.exception.BaseException
import kr.hanchae.moyeotrip.exception.ErrorCode
import org.slf4j.LoggerFactory
import org.springframework.ai.image.ImageModel
import org.springframework.ai.image.ImagePrompt
import org.springframework.stereotype.Component
import java.util.Base64

@Component
class ProfileImageGenerationClient(
    private val imageModel: ImageModel,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun generate(prompt: String): ByteArray =
        try {
            val encodedImage =
                imageModel
                    .call(ImagePrompt(prompt))
                    .result
                    ?.output
                    ?.b64Json
                    ?: throw IllegalStateException("이미지 생성 결과가 비어 있습니다.")
            Base64.getDecoder().decode(encodedImage)
        } catch (exception: Exception) {
            log.warn("AI 프로필 이미지 생성에 실패했습니다.", exception)
            throw BaseException(ErrorCode.PROFILE_IMAGE_GENERATION_FAILED, ErrorCode.PROFILE_IMAGE_GENERATION_FAILED.errorMessage)
        }
}
