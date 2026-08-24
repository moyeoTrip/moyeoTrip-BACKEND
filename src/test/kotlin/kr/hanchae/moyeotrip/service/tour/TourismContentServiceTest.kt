package kr.hanchae.moyeotrip.service.tour

import kr.hanchae.moyeotrip.client.TourApiClient
import kr.hanchae.moyeotrip.exception.BaseException
import kr.hanchae.moyeotrip.exception.ErrorCode
import kr.hanchae.moyeotrip.repository.TourismContentImageRepository
import kr.hanchae.moyeotrip.repository.TourismContentRepository
import kr.hanchae.moyeotrip.repository.TourismContentTypeRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort

class TourismContentServiceTest {
    private val tourApiClient = mock(TourApiClient::class.java)
    private val repository = mock(TourismContentRepository::class.java)
    private val contentTypeRepository = mock(TourismContentTypeRepository::class.java)
    private val imageRepository = mock(TourismContentImageRepository::class.java)
    private val service = TourismContentService(tourApiClient, repository, contentTypeRepository, imageRepository)

    @Test
    fun `여행지 검색어는 앞뒤 공백을 제거하고 제목과 주소 검색 패턴으로 전달한다`() {
        val pageable = PageRequest.of(0, 20, Sort.by("title").ascending())
        `when`(repository.searchListableContents(25, 12, "%주왕산%", pageable)).thenReturn(PageImpl(emptyList()))

        val response = service.getContents(contentTypeId = 12, keyword = "  주왕산  ", page = 0, size = 20)

        assertEquals(0, response.totalElements)
        verify(repository).searchListableContents(25, 12, "%주왕산%", pageable)
    }

    @Test
    fun `빈 검색어는 전체 조회 조건으로 전달한다`() {
        val pageable = PageRequest.of(0, 20, Sort.by("title").ascending())
        `when`(repository.searchListableContents(25, null, null, pageable)).thenReturn(PageImpl(emptyList()))

        service.getContents(contentTypeId = null, keyword = "  ", page = 0, size = 20)

        verify(repository).searchListableContents(25, null, null, pageable)
    }

    @Test
    fun `여행 코스 타입은 검색할 수 없다`() {
        val exception =
            assertThrows(BaseException::class.java) {
                service.getContents(contentTypeId = 25, keyword = null, page = 0, size = 20)
            }

        assertEquals(ErrorCode.TOURISM_COURSE_CONTENT_NOT_LISTED, exception.errorCode)
        verifyNoInteractions(repository)
    }
}
