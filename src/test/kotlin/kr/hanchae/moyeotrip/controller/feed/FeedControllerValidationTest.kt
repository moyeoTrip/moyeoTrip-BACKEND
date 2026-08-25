package kr.hanchae.moyeotrip.controller.feed

import kr.hanchae.moyeotrip.service.feed.FeedService
import kr.hanchae.moyeotrip.support.LoginUserIdStubResolver
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verifyNoInteractions
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class FeedControllerValidationTest {
    private val feedService = mock(FeedService::class.java)
    private val mockMvc: MockMvc =
        MockMvcBuilders
            .standaloneSetup(FeedController(feedService))
            .setCustomArgumentResolvers(LoginUserIdStubResolver())
            .build()

    @Test
    fun `피드 작성은 공백 감상평을 거부한다`() {
        val request = MockMultipartFile("request", "request.json", "application/json", INVALID_FEED_JSON.toByteArray())
        val image = MockMultipartFile("images", "trip.jpg", "image/jpeg", byteArrayOf(1))

        mockMvc
            .perform(multipart("/api/v1/feeds").file(request).file(image))
            .andExpect(status().isBadRequest)

        verifyNoInteractions(feedService)
    }

    @Test
    fun `피드 댓글은 500자를 초과할 수 없다`() {
        val tooLong = "가".repeat(501)

        mockMvc
            .perform(
                post("/api/v1/feeds/10/comments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"content":"$tooLong"}"""),
            ).andExpect(status().isBadRequest)

        verifyNoInteractions(feedService)
    }

    companion object {
        private const val INVALID_FEED_JSON =
            """{"chatRoomId":10,"content":"   ","visibility":"PUBLIC"}"""
    }
}
