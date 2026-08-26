package kr.hanchae.moyeotrip.service.tour

import kr.hanchae.moyeotrip.client.TourApiClient
import kr.hanchae.moyeotrip.client.TourAreaBasedItem
import kr.hanchae.moyeotrip.client.TourAreaBasedPage
import kr.hanchae.moyeotrip.entity.tour.TourismContent
import kr.hanchae.moyeotrip.entity.tour.TourismContentType
import kr.hanchae.moyeotrip.exception.BaseException
import kr.hanchae.moyeotrip.exception.ErrorCode
import kr.hanchae.moyeotrip.repository.ObjectStorageRepository
import kr.hanchae.moyeotrip.repository.TourismContentRepository
import kr.hanchae.moyeotrip.repository.TourismContentTypeRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.time.LocalDateTime

class TourismContentSyncServiceTest {
    private val tourApiClient = mock(TourApiClient::class.java)
    private val repository = mock(TourismContentRepository::class.java)
    private val contentTypeRepository = mock(TourismContentTypeRepository::class.java)
    private val tourismImageProxyService = mock(TourismImageProxyService::class.java)
    private val objectStorageRepository = mock(ObjectStorageRepository::class.java)
    private val service =
        TourismContentSyncService(
            tourApiClient,
            repository,
            contentTypeRepository,
            tourismImageProxyService,
            objectStorageRepository,
        )

    @Test
    fun `관광정보는 페이지를 모두 조회하고 중복 제거 후 기존 데이터와 신규 데이터를 저장한다`() {
        val contentType = TourismContentType(12, "관광지")
        val existing = TourismContent(contentId = 100L, contentType = contentType, title = "이전 제목")
        val updated = areaItem(100L, "새 제목", createdTime = "20260822182738")
        val duplicate = areaItem(100L, "중복 제목")
        val newItem = areaItem(101L, "신규 관광지", createdTime = "잘못된 날짜")
        `when`(contentTypeRepository.findAll()).thenReturn(listOf(contentType))
        `when`(tourApiClient.getAreaBasedTourismInformation(12, 1, 1000))
            .thenReturn(TourAreaBasedPage(listOf(updated, duplicate), 1001))
        `when`(tourApiClient.getAreaBasedTourismInformation(12, 2, 1000))
            .thenReturn(TourAreaBasedPage(listOf(newItem), 1001))
        `when`(repository.findAll()).thenReturn(listOf(existing))
        val captor = iterableCaptor<TourismContent>()

        val count = service.syncGyeongsangbukdo()

        assertEquals(2, count)
        assertEquals("새 제목", existing.title)
        assertEquals(LocalDateTime.of(2026, 8, 22, 18, 27, 38), existing.sourceCreatedDateTime)
        verify(repository).saveAll(captor.capture())
        assertNull(captor.value.single { it.contentId == 101L }.sourceCreatedDateTime)
    }

    @Test
    fun `API 응답의 관광 타입이 DB에 없으면 동기화를 중단한다`() {
        val contentType = TourismContentType(12, "관광지")
        `when`(contentTypeRepository.findAll()).thenReturn(listOf(contentType))
        `when`(tourApiClient.getAreaBasedTourismInformation(12, 1, 1000))
            .thenReturn(TourAreaBasedPage(listOf(areaItem(100L, "숙박", contentTypeId = 32)), 1))
        `when`(repository.findAll()).thenReturn(emptyList())

        val exception = assertThrows(BaseException::class.java) { service.syncGyeongsangbukdo() }

        assertEquals(ErrorCode.TOURISM_CONTENT_TYPE_NOT_FOUND, exception.errorCode)
    }

    private fun areaItem(
        contentId: Long,
        title: String,
        contentTypeId: Int = 12,
        createdTime: String = "",
    ) = TourAreaBasedItem(
        contentid = contentId.toString(),
        contenttypeid = contentTypeId.toString(),
        title = title,
        addr1 = " 경상북도 경주시 ",
        addr2 = " ",
        mapx = "129.2247",
        mapy = "35.8562",
        createdtime = createdTime,
    )

    @Suppress("UNCHECKED_CAST")
    private fun <T> iterableCaptor(): ArgumentCaptor<Iterable<T>> =
        ArgumentCaptor.forClass(Iterable::class.java) as ArgumentCaptor<Iterable<T>>
}
