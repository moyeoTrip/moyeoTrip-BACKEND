package kr.hanchae.moyeotrip.service.tour

import kr.hanchae.moyeotrip.client.TourApiClient
import kr.hanchae.moyeotrip.client.TourLegalDongItem
import kr.hanchae.moyeotrip.client.TourLegalDongPage
import kr.hanchae.moyeotrip.entity.tour.LegalDongCode
import kr.hanchae.moyeotrip.repository.LegalDongCodeRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class LegalDongCodeSyncServiceTest {
    private val tourApiClient = mock(TourApiClient::class.java)
    private val repository = mock(LegalDongCodeRepository::class.java)
    private val service = LegalDongCodeSyncService(tourApiClient, repository)

    @Test
    fun `한 페이지의 법정동 코드는 새 엔티티로 저장한다`() {
        val item = legalDong("47110", "포항시")
        `when`(tourApiClient.getGyeongsangbukdoLegalDongCodes(1, 1000))
            .thenReturn(TourLegalDongPage(listOf(item), 1))
        `when`(repository.findAllByRegionCode("47")).thenReturn(emptyList())
        val captor = iterableCaptor<LegalDongCode>()

        val count = service.syncGyeongsangbukdo()

        assertEquals(1, count)
        verify(repository).saveAll(captor.capture())
        assertEquals("포항시", captor.value.single().signguName)
    }

    @Test
    fun `여러 페이지를 조회하고 기존 법정동 이름을 갱신한다`() {
        val existing = LegalDongCode(regionCode = "47", signguCode = "47110", regionName = "이전", signguName = "이전")
        `when`(tourApiClient.getGyeongsangbukdoLegalDongCodes(1, 1000))
            .thenReturn(TourLegalDongPage(listOf(legalDong("47110", "포항시")), 1001))
        `when`(tourApiClient.getGyeongsangbukdoLegalDongCodes(2, 1000))
            .thenReturn(TourLegalDongPage(listOf(legalDong("47130", "경주시")), 1001))
        `when`(repository.findAllByRegionCode("47")).thenReturn(listOf(existing))

        val count = service.syncGyeongsangbukdo()

        assertEquals(2, count)
        assertEquals("경상북도", existing.regionName)
        assertEquals("포항시", existing.signguName)
        verify(tourApiClient).getGyeongsangbukdoLegalDongCodes(2, 1000)
    }

    private fun legalDong(
        signguCode: String,
        signguName: String,
    ) = TourLegalDongItem("47", "경상북도", signguCode, signguName)

    @Suppress("UNCHECKED_CAST")
    private fun <T> iterableCaptor(): ArgumentCaptor<Iterable<T>> =
        ArgumentCaptor.forClass(Iterable::class.java) as ArgumentCaptor<Iterable<T>>
}
