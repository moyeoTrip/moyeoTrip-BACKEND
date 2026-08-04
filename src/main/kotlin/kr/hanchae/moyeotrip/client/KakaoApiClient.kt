package kr.hanchae.moyeotrip.client

import kr.hanchae.moyeotrip.config.properties.KakaoProperties
import kr.hanchae.moyeotrip.controller.client.KakaoOAuthTokenResponse
import kr.hanchae.moyeotrip.controller.client.KakaoTokenInfoResponse
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestClient
import org.springframework.web.client.body

interface KakaoClient {
    fun getTokenInfo(accessToken: String): KakaoTokenInfoResponse

    fun exchangeAuthorizationCode(
        code: String,
        redirectUri: String,
    ): String
}

@Component
class KakaoApiClient(
    restClientBuilder: RestClient.Builder,
    private val kakaoProperties: KakaoProperties,
) : KakaoClient {
    private val kakaoApiClient = restClientBuilder.clone().baseUrl("https://kapi.kakao.com").build()
    private val kakaoAuthClient = restClientBuilder.clone().baseUrl("https://kauth.kakao.com").build()

    override fun getTokenInfo(accessToken: String): KakaoTokenInfoResponse =
        kakaoApiClient
            .get()
            .uri("/v1/user/access_token_info")
            .header(AUTHORIZATION, "Bearer $accessToken")
            .retrieve()
            .body<KakaoTokenInfoResponse>()
            ?: error("카카오 토큰 정보 응답이 비어 있습니다.")

    override fun exchangeAuthorizationCode(
        code: String,
        redirectUri: String,
    ): String {
        val form =
            LinkedMultiValueMap<String, String>().apply {
                add("grant_type", "authorization_code")
                add("client_id", kakaoProperties.restApiKey)
                add("redirect_uri", redirectUri)
                add("code", code)
                kakaoProperties.clientSecret.takeIf { it.isNotBlank() }?.let { add("client_secret", it) }
            }

        return kakaoAuthClient
            .post()
            .uri("/oauth/token")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(form)
            .retrieve()
            .body<KakaoOAuthTokenResponse>()
            ?.accessToken
            ?.takeIf { it.isNotBlank() }
            ?: error("카카오 토큰 발급 응답이 비어 있습니다.")
    }
}
