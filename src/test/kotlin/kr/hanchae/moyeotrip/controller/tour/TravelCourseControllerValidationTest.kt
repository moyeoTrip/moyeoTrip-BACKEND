package kr.hanchae.moyeotrip.controller.tour

import kr.hanchae.moyeotrip.service.chat.ChatRoomService
import kr.hanchae.moyeotrip.service.tour.TravelCourseService
import kr.hanchae.moyeotrip.support.LoginUserIdStubResolver
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verifyNoInteractions
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class TravelCourseControllerValidationTest {
    private val chatRoomService = mock(ChatRoomService::class.java)
    private val travelCourseService = mock(TravelCourseService::class.java)
    private val mockMvc: MockMvc =
        MockMvcBuilders
            .standaloneSetup(TravelCourseController(chatRoomService, travelCourseService))
            .setCustomArgumentResolvers(LoginUserIdStubResolver())
            .build()

    @Test
    fun `코스 수정은 방문지 두 개 미만을 거부한다`() {
        putJson(
            "/api/v1/travel-courses/chat-rooms/10",
            """{"places":[{"contentId":1,"dayNumber":1,"sequence":1,"visitTime":"09:00"}]}""",
        ).andExpect(status().isBadRequest)
        verifyNoInteractions(chatRoomService)
    }

    @Test
    fun `코스 수정은 중첩 방문지의 음수 순서를 거부한다`() {
        putJson(
            "/api/v1/travel-courses/chat-rooms/10",
            """
            {"places":[
              {"contentId":1,"dayNumber":1,"sequence":0,"visitTime":"09:00"},
              {"contentId":2,"dayNumber":1,"sequence":2,"visitTime":"11:00"}
            ]}
            """.trimIndent(),
        ).andExpect(status().isBadRequest)
        verifyNoInteractions(chatRoomService)
    }

    @Test
    fun `코스 공개는 공백 제목과 설명을 거부한다`() {
        postJson(
            "/api/v1/travel-courses/10/publication",
            """{"title":" ","description":" ","showCreatorNickname":true}""",
        ).andExpect(status().isBadRequest)
        verifyNoInteractions(travelCourseService)
    }

    @Test
    fun `코스 평점은 1에서 5 사이여야 한다`() {
        postJson("/api/v1/travel-courses/chat-rooms/10/rating", """{"score":6}""")
            .andExpect(status().isBadRequest)
        verifyNoInteractions(chatRoomService)
    }

    private fun putJson(
        path: String,
        json: String,
    ) = mockMvc.perform(put(path).contentType(MediaType.APPLICATION_JSON).content(json))

    private fun postJson(
        path: String,
        json: String,
    ) = mockMvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(json))
}
