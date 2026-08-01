package kr.hanchae.moyeotrip.client

import kr.hanchae.moyeotrip.controller.client.KakaoTokenInfoResponse
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.body

interface KakaoClient {
    fun getTokenInfo(accessToken: String): KakaoTokenInfoResponse
}

@Component
class KakaoApiClient(
    restClientBuilder: RestClient.Builder,
) : KakaoClient {
    private val restClient = restClientBuilder.baseUrl("https://kapi.kakao.com").build()

    override fun getTokenInfo(accessToken: String): KakaoTokenInfoResponse =
        restClient
            .get()
            .uri("/v1/user/access_token_info")
            .header(AUTHORIZATION, "Bearer $accessToken")
            .retrieve()
            .body<KakaoTokenInfoResponse>()
            ?: error("카카오 토큰 정보 응답이 비어 있습니다.")
}
