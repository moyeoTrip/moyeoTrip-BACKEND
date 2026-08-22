package kr.hanchae.moyeotrip.controller.auth

import kr.hanchae.moyeotrip.controller.auth.request.KakaoAuthorizationCodeRequest
import kr.hanchae.moyeotrip.controller.auth.response.FirebaseCustomTokenResponse
import kr.hanchae.moyeotrip.service.auth.AuthService
import kr.hanchae.moyeotrip.service.auth.NicknameCandidateService
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class AuthControllerContractTest {
    private val authService = mock(AuthService::class.java)
    private val mockMvc: MockMvc =
        MockMvcBuilders
            .standaloneSetup(AuthController(authService, mock(NicknameCandidateService::class.java)))
            .build()

    @Test
    fun `Web Kakao authorization-code API는 명시적 DTO를 받아 Firebase custom token을 반환한다`() {
        val request =
            KakaoAuthorizationCodeRequest(
                code = "authorization-code",
                redirectUri = "https://moyeo-trip.jayden-bin.cc/moyeoTrip-Web/auth/kakao/callback",
            )
        `when`(authService.createKakaoCustomToken(request))
            .thenReturn(FirebaseCustomTokenResponse("firebase-custom-token"))

        mockMvc
            .perform(
                post("/api/v1/auth/firebase/kakao/authorization-code/custom-token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "code": "authorization-code",
                          "redirectUri": "https://moyeo-trip.jayden-bin.cc/moyeoTrip-Web/auth/kakao/callback"
                        }
                        """.trimIndent(),
                    ),
            ).andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.customToken").value("firebase-custom-token"))
    }

    @Test
    fun `Web Kakao authorization-code API는 빈 인가 코드를 거부한다`() {
        mockMvc
            .perform(
                post("/api/v1/auth/firebase/kakao/authorization-code/custom-token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "code": "",
                          "redirectUri": "https://moyeo-trip.jayden-bin.cc/moyeoTrip-Web/auth/kakao/callback"
                        }
                        """.trimIndent(),
                    ),
            ).andExpect(status().isBadRequest)
    }
}
