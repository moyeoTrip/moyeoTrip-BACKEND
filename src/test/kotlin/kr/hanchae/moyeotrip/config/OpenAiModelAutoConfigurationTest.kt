package kr.hanchae.moyeotrip.config

import com.openai.client.OpenAIClient
import org.junit.jupiter.api.Test
import org.springframework.ai.model.openai.autoconfigure.OpenAiAudioSpeechAutoConfiguration
import org.springframework.ai.model.openai.autoconfigure.OpenAiAudioTranscriptionAutoConfiguration
import org.springframework.ai.model.openai.autoconfigure.OpenAiImageAutoConfiguration
import org.springframework.ai.model.openai.autoconfigure.OpenAiModerationAutoConfiguration
import org.springframework.ai.openai.OpenAiAudioSpeechModel
import org.springframework.ai.openai.OpenAiAudioTranscriptionModel
import org.springframework.ai.openai.OpenAiImageModel
import org.springframework.ai.openai.OpenAiModerationModel
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OpenAiModelAutoConfigurationTest {
    private val contextRunner =
        ApplicationContextRunner()
            .withConfiguration(
                AutoConfigurations.of(
                    OpenAiImageAutoConfiguration::class.java,
                    OpenAiAudioSpeechAutoConfiguration::class.java,
                    OpenAiAudioTranscriptionAutoConfiguration::class.java,
                    OpenAiModerationAutoConfiguration::class.java,
                ),
            ).withUserConfiguration(ProfileImageOpenAiConfiguration::class.java)
            .withPropertyValues(
                "spring.ai.model.image=openai",
                "spring.ai.model.audio.speech=none",
                "spring.ai.model.audio.transcription=none",
                "spring.ai.model.moderation=none",
                "spring.ai.openai.image.api-key=test-api-key",
                "spring.ai.openai.image.model=gpt-image-2",
                "spring.ai.openai.image.quality=low",
                "spring.ai.openai.image.n=1",
                "spring.ai.openai.image.size=1024x1024",
            )

    @Test
    fun `image model만 자동 구성한다`() {
        contextRunner.run { context ->
            assertEquals(1, context.getBeansOfType(OpenAiImageModel::class.java).size)
            assertTrue(context.getBeansOfType(OpenAiAudioSpeechModel::class.java).isEmpty())
            assertTrue(context.getBeansOfType(OpenAiAudioTranscriptionModel::class.java).isEmpty())
            assertTrue(context.getBeansOfType(OpenAiModerationModel::class.java).isEmpty())
            assertEquals(1, context.getBeansOfType(OpenAIClient::class.java).size)
        }
    }
}
