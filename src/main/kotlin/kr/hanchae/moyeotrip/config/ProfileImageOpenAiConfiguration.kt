package kr.hanchae.moyeotrip.config

import com.openai.client.OpenAIClient
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.observation.ObservationRegistry
import org.springframework.ai.model.openai.autoconfigure.OpenAiAutoConfigurationUtil
import org.springframework.ai.model.openai.autoconfigure.OpenAiCommonProperties
import org.springframework.ai.model.openai.autoconfigure.OpenAiImageProperties
import org.springframework.ai.openai.http.okhttp.OpenAiHttpClientBuilderCustomizer
import org.springframework.ai.openai.setup.OpenAiSetup
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

const val PROFILE_IMAGE_OPEN_AI_CLIENT = "profileImageOpenAiClient"

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(OpenAiCommonProperties::class, OpenAiImageProperties::class)
class ProfileImageOpenAiConfiguration {
    @Bean(PROFILE_IMAGE_OPEN_AI_CLIENT)
    fun profileImageOpenAiClient(
        commonProperties: OpenAiCommonProperties,
        imageProperties: OpenAiImageProperties,
        observationRegistry: ObjectProvider<ObservationRegistry>,
        meterRegistry: ObjectProvider<MeterRegistry>,
        httpClientBuilderCustomizers: ObjectProvider<OpenAiHttpClientBuilderCustomizer>,
    ): OpenAIClient {
        val properties = OpenAiAutoConfigurationUtil.resolveCommonProperties(commonProperties, imageProperties)
        val meterRegistryToUse =
            if (properties.isConnectionPoolMetricsEnabled) {
                meterRegistry.getIfAvailable()
            } else {
                null
            }
        return OpenAiSetup.setupSyncClient(
            properties.baseUrl,
            properties.apiKey,
            properties.credential,
            properties.microsoftDeploymentName,
            properties.microsoftFoundryServiceVersion,
            properties.organizationId,
            properties.isMicrosoftFoundry,
            properties.isGitHubModels,
            properties.model,
            properties.timeout,
            properties.maxRetries,
            properties.proxy,
            properties.customHeaders,
            observationRegistry.getIfUnique { ObservationRegistry.NOOP },
            meterRegistryToUse,
            httpClientBuilderCustomizers.orderedStream().toList(),
        )
    }
}
