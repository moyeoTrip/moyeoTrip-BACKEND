package kr.hanchae.moyeotrip.controller.chat

import kr.hanchae.moyeotrip.controller.chat.request.CreateChatRoomRequest
import kr.hanchae.moyeotrip.controller.chat.response.CreateChatRoomResponse
import kr.hanchae.moyeotrip.service.chat.ChatRoomService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.http.HttpStatus

class ChatRoomControllerContractTest {
    private val chatRoomService = mock(ChatRoomService::class.java)
    private val controller = ChatRoomController(chatRoomService)

    @Test
    fun `채팅방 생성은 생성된 roomId 본문의 201 응답을 반환한다`() {
        val request = mock(CreateChatRoomRequest::class.java)
        `when`(chatRoomService.createRoom(7L, request, null)).thenReturn(CreateChatRoomResponse(roomId = 101L))

        val response = controller.createRoom(7L, request, null)

        assertEquals(HttpStatus.CREATED, response.statusCode)
        assertEquals(CreateChatRoomResponse(roomId = 101L), response.body)
        verify(chatRoomService).createRoom(7L, request, null)
    }
}
