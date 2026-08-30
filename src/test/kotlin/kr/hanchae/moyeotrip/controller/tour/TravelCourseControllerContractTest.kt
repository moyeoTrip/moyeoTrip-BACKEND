package kr.hanchae.moyeotrip.controller.tour

import kr.hanchae.moyeotrip.service.chat.ChatRoomService
import kr.hanchae.moyeotrip.service.search.PopularSearchKeywordService
import kr.hanchae.moyeotrip.service.tour.TravelCourseService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class TravelCourseControllerContractTest {
    private val chatRoomService = mock(ChatRoomService::class.java)
    private val travelCourseService = mock(TravelCourseService::class.java)
    private val popularSearchKeywordService = mock(PopularSearchKeywordService::class.java)
    private val controller = TravelCourseController(chatRoomService, travelCourseService, popularSearchKeywordService)

    @Test
    fun `공개 코스 검색어를 모임 검색과 같은 인기 검색어 집계에 전달한다`() {
        `when`(chatRoomService.searchPublicCourses(" 경주 야경 ")).thenReturn(emptyList())

        val response = controller.searchPublicCourses(" 경주 야경 ")

        assertEquals(emptyList<Any>(), response)
        verify(chatRoomService).searchPublicCourses(" 경주 야경 ")
        verify(popularSearchKeywordService).record(" 경주 야경 ")
    }
}
