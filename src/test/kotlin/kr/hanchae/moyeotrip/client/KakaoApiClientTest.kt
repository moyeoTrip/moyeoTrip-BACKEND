package kr.hanchae.moyeotrip.client

import kr.hanchae.moyeotrip.config.properties.KakaoProperties
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.content
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient

class KakaoApiClientTest {
    @Test
    fun `인가 코드를 form body로 교환하며 설정된 client secret을 포함한다`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val client =
            KakaoApiClient(
                builder,
                KakaoProperties(
                    appId = 1234L,
                    restApiKey = "rest-api-key",
                    clientSecret = "client-secret",
                    allowedRedirectUris = emptyList(),
                ),
            )
        server
            .expect(requestTo("https://kauth.kakao.com/oauth/token"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
            .andExpect(content().string(containsString("grant_type=authorization_code")))
            .andExpect(content().string(containsString("client_id=rest-api-key")))
            .andExpect(content().string(containsString("redirect_uri=https%3A%2F%2Fexample.com%2Fcallback")))
            .andExpect(content().string(containsString("code=authorization-code")))
            .andExpect(content().string(containsString("client_secret=client-secret")))
            .andRespond(withSuccess("""{"access_token":"kakao-access-token"}""", MediaType.APPLICATION_JSON))

        val accessToken = client.exchangeAuthorizationCode("authorization-code", "https://example.com/callback")

        assertEquals("kakao-access-token", accessToken)
        server.verify()
    }

    @Test
    fun `비활성 client secret은 토큰 교환 요청에서 생략한다`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val client =
            KakaoApiClient(
                builder,
                KakaoProperties(
                    appId = 1234L,
                    restApiKey = "rest-api-key",
                    clientSecret = " ",
                    allowedRedirectUris = emptyList(),
                ),
            )
        server
            .expect(requestTo("https://kauth.kakao.com/oauth/token"))
            .andExpect(content().string(org.hamcrest.Matchers.not(containsString("client_secret"))))
            .andRespond(withSuccess("""{"access_token":"kakao-access-token"}""", MediaType.APPLICATION_JSON))

        client.exchangeAuthorizationCode("authorization-code", "https://example.com/callback")

        server.verify()
    }
}
