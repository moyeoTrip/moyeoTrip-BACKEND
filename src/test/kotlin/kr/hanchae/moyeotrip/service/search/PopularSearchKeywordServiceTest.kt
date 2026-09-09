package kr.hanchae.moyeotrip.service.search

import kr.hanchae.moyeotrip.controller.search.response.PopularSearchRankTrend
import kr.hanchae.moyeotrip.repository.PopularSearchKeyword
import kr.hanchae.moyeotrip.repository.PopularSearchKeywordRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`

class PopularSearchKeywordServiceTest {
    private val repository = mock(PopularSearchKeywordRepository::class.java)
    private val service = PopularSearchKeywordService(repository)

    @Test
    fun `검색어의 공백과 대소문자를 정규화해 집계한다`() {
        service.record("  Gyeongju   야경  ")

        verify(repository).increment("gyeongju 야경")
    }

    @Test
    fun `빈 검색어는 집계하지 않는다`() {
        service.record("   ")
        service.record(null)

        verifyNoInteractions(repository)
    }

    @Test
    fun `인기 검색어를 순위와 전일 대비 등락으로 변환한다`() {
        `when`(repository.findTop(5)).thenReturn(
            listOf(
                PopularSearchKeyword("주왕산", 12, 3),
                PopularSearchKeyword("경주 야경", 8, 1),
                PopularSearchKeyword("안동", 7, 3),
                PopularSearchKeyword("영덕", 6, null),
            ),
        )

        val response = service.getPopularKeywords(5)

        assertEquals(1, response[0].rank)
        assertEquals("주왕산", response[0].keyword)
        assertEquals(12, response[0].searchCount)
        assertEquals(PopularSearchRankTrend.UP, response[0].rankTrend)
        assertEquals(2, response[0].rankChange)
        assertEquals(PopularSearchRankTrend.DOWN, response[1].rankTrend)
        assertEquals(-1, response[1].rankChange)
        assertEquals(PopularSearchRankTrend.SAME, response[2].rankTrend)
        assertEquals(0, response[2].rankChange)
        assertEquals(PopularSearchRankTrend.NEW, response[3].rankTrend)
        assertEquals(null, response[3].rankChange)
    }

    @Test
    fun `Redis 집계와 조회 실패는 사용자 검색 요청을 실패시키지 않는다`() {
        doThrow(IllegalStateException("redis down")).`when`(repository).increment("주왕산")
        `when`(repository.findTop(10)).thenThrow(IllegalStateException("redis down"))

        service.record("주왕산")

        assertEquals(emptyList<Any>(), service.getPopularKeywords(10))
    }
}
