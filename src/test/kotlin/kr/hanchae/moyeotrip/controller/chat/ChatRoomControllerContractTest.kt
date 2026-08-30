package kr.hanchae.moyeotrip.controller.chat

import kr.hanchae.moyeotrip.controller.chat.request.CreateChatRoomNoticeRequest
import kr.hanchae.moyeotrip.controller.chat.request.CreateChatRoomRequest
import kr.hanchae.moyeotrip.controller.chat.response.CreateChatRoomNoticeResponse
import kr.hanchae.moyeotrip.controller.chat.response.CreateChatRoomResponse
import kr.hanchae.moyeotrip.service.chat.ChatRoomService
import kr.hanchae.moyeotrip.service.search.PopularSearchKeywordService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.http.HttpStatus
import org.springframework.web.multipart.MultipartFile

class ChatRoomControllerContractTest {
    private val chatRoomService = mock(ChatRoomService::class.java)
    private val popularSearchKeywordService = mock(PopularSearchKeywordService::class.java)
    private val controller = ChatRoomController(chatRoomService, popularSearchKeywordService)

    @Test
    fun `채팅방 생성은 생성된 roomId 본문의 201 응답을 반환한다`() {
        val request = mock(CreateChatRoomRequest::class.java)
        val thumbnail = mock(MultipartFile::class.java)
        `when`(chatRoomService.createRoom(7L, request, thumbnail)).thenReturn(CreateChatRoomResponse(roomId = 101L))

        val response = controller.createRoom(7L, request, thumbnail)

        assertEquals(HttpStatus.CREATED, response.statusCode)
        assertEquals(CreateChatRoomResponse(roomId = 101L), response.body)
        verify(chatRoomService).createRoom(7L, request, thumbnail)
    }

    @Test
    fun `공지 생성은 생성된 noticeId 본문의 201 응답을 반환한다`() {
        val request = CreateChatRoomNoticeRequest(notice = "준비물 공지", pinned = true)
        `when`(chatRoomService.createNotice(7L, 101L, "준비물 공지", true)).thenReturn(44L)

        val response = controller.createNotice(7L, 101L, request)

        assertEquals(HttpStatus.CREATED, response.statusCode)
        assertEquals(CreateChatRoomNoticeResponse(noticeId = 44L), response.body)
        verify(chatRoomService).createNotice(7L, 101L, "준비물 공지", true)
    }

    @Test
    fun `모임 통합 검색어를 인기 검색어 집계에 전달한다`() {
        `when`(chatRoomService.searchRooms(7L, " 주왕산 ", 20)).thenReturn(emptyList())

        controller.searchRooms(7L, " 주왕산 ", 20)

        verify(chatRoomService).searchRooms(7L, " 주왕산 ", 20)
        verify(popularSearchKeywordService).record(" 주왕산 ")
    }
}
