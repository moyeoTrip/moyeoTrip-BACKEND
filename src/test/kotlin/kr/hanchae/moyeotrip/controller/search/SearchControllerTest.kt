package kr.hanchae.moyeotrip.controller.search

import kr.hanchae.moyeotrip.service.search.PopularSearchKeywordService
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

class SearchControllerTest {
    private val service = mock(PopularSearchKeywordService::class.java)
    private val controller = SearchController(service)

    @Test
    fun `인기 검색어 조회 개수는 1에서 20 사이로 제한한다`() {
        controller.getPopularKeywords(0)
        controller.getPopularKeywords(100)

        verify(service).getPopularKeywords(1)
        verify(service).getPopularKeywords(20)
    }
}
