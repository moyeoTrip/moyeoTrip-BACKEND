package kr.hanchae.moyeotrip.controller.auth

import kr.hanchae.moyeotrip.service.auth.AuthService
import kr.hanchae.moyeotrip.service.auth.NicknameCandidateService
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verifyNoInteractions
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class AuthControllerValidationTest {
    private val authService = mock(AuthService::class.java)
    private val nicknameService = mock(NicknameCandidateService::class.java)
    private val mockMvc: MockMvc = MockMvcBuilders.standaloneSetup(AuthController(authService, nicknameService)).build()

    @Test
    fun `카카오 액세스 토큰은 공백일 수 없다`() {
        postJson("/api/v1/auth/firebase/kakao/custom-token", """{"accessToken":" "}""")
            .andExpect(status().isBadRequest)
        verifyNoInteractions(authService)
    }

    @Test
    fun `카카오 인가 코드와 redirect URI는 길이를 검증한다`() {
        val tooLong = "a".repeat(2049)
        postJson(
            "/api/v1/auth/firebase/kakao/authorization-code/custom-token",
            """{"code":"$tooLong","redirectUri":" "}""",
        ).andExpect(status().isBadRequest)
        verifyNoInteractions(authService)
    }

    @Test
    fun `Firebase 로그인 ID 토큰은 공백일 수 없다`() {
        postJson("/api/v1/auth/login", """{"idToken":" "}""")
            .andExpect(status().isBadRequest)
        verifyNoInteractions(authService)
    }

    @Test
    fun `회원가입은 닉네임 길이와 과거 생년월일을 검증한다`() {
        postJson(
            "/api/v1/auth/signup",
            """
            {
              "idToken":"token",
              "nicknameSelectionToken":"selection",
              "nickname":"가",
              "gender":"F",
              "birthDate":"2999-01-01"
            }
            """.trimIndent(),
        ).andExpect(status().isBadRequest)
        verifyNoInteractions(authService)
    }

    @Test
    fun `토큰 재발급 요청의 refresh token은 공백일 수 없다`() {
        postJson("/api/v1/auth/refresh", """{"refreshToken":""}""")
            .andExpect(status().isBadRequest)
        verifyNoInteractions(authService)
    }

    private fun postJson(
        path: String,
        json: String,
    ) = mockMvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(json))
}
