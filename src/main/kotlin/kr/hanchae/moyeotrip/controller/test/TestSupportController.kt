package kr.hanchae.moyeotrip.controller.test

import kr.hanchae.moyeotrip.controller.auth.response.TestAccessTokenResponse
import kr.hanchae.moyeotrip.controller.test.response.TestCompletedChatRoomResponse
import kr.hanchae.moyeotrip.service.test.TestSupportService
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class TestSupportController(
    private val testSupportService: TestSupportService,
) : TestSupportAPISpec {
    @PostMapping("/api/v1/auth/test-token/{userId}")
    override fun issueAccessToken(
        @PathVariable userId: Long,
    ): TestAccessTokenResponse = testSupportService.issueAccessToken(userId)

    @PostMapping("/api/v1/test/chat-rooms/{roomId}/complete")
    override fun completeChatRoom(
        @PathVariable roomId: Long,
    ): TestCompletedChatRoomResponse = testSupportService.completeChatRoom(roomId)
}
