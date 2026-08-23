package kr.hanchae.moyeotrip.controller.user

import kr.hanchae.moyeotrip.service.auth.UserService
import kr.hanchae.moyeotrip.service.user.TravelCompanionService
import kr.hanchae.moyeotrip.support.LoginUserIdStubResolver
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verifyNoInteractions
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class UserControllerValidationTest {
    private val userService = mock(UserService::class.java)
    private val travelCompanionService = mock(TravelCompanionService::class.java)

    @Test
    fun `프로필 수정은 미래 생년월일과 300자 초과 자기소개를 거부한다`() {
        val mockMvc = mockMvc(UserController(userService))
        val introduction = "가".repeat(301)

        putJson(
            mockMvc,
            "/api/v1/users/me/profile",
            """{"introduction":"$introduction","birthDate":"2999-01-01","gender":"F"}""",
        ).andExpect(status().isBadRequest)
        verifyNoInteractions(userService)
    }

    @Test
    fun `프로필 이미지 선택 ID는 양수여야 한다`() {
        val mockMvc = mockMvc(UserController(userService))

        putJson(mockMvc, "/api/v1/users/me/profile-image", """{"profileImageId":0}""")
            .andExpect(status().isBadRequest)
        verifyNoInteractions(userService)
    }

    @Test
    fun `동행자 평가는 점수 범위와 한줄평 길이를 검증한다`() {
        val mockMvc = mockMvc(TravelCompanionController(travelCompanionService))
        val review = "가".repeat(41)

        putJson(
            mockMvc,
            "/api/v1/chat-rooms/10/companions/2/review",
            """{"mannerScore":0,"oneLineReview":"$review"}""",
        ).andExpect(status().isBadRequest)
        verifyNoInteractions(travelCompanionService)
    }

    private fun mockMvc(controller: Any): MockMvc =
        MockMvcBuilders
            .standaloneSetup(controller)
            .setCustomArgumentResolvers(LoginUserIdStubResolver())
            .build()

    private fun putJson(
        mockMvc: MockMvc,
        path: String,
        json: String,
    ) = mockMvc.perform(put(path).contentType(MediaType.APPLICATION_JSON).content(json))
}
