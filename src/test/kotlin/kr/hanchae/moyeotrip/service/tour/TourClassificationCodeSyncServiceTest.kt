package kr.hanchae.moyeotrip.service.tour

import kr.hanchae.moyeotrip.client.TourApiClient
import kr.hanchae.moyeotrip.client.TourClassificationSystemItem
import kr.hanchae.moyeotrip.client.TourClassificationSystemPage
import kr.hanchae.moyeotrip.entity.tour.TourClassificationCode
import kr.hanchae.moyeotrip.repository.TourClassificationCodeRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class TourClassificationCodeSyncServiceTest {
    private val tourApiClient = mock(TourApiClient::class.java)
    private val repository = mock(TourClassificationCodeRepository::class.java)
    private val service = TourClassificationCodeSyncService(tourApiClient, repository)

    @Test
    fun `분류 코드는 모든 페이지를 읽고 기존 이름을 갱신한다`() {
        val existing = classificationEntity(level3Name = "이전 이름")
        `when`(tourApiClient.getClassificationSystemCodes(1, 1000))
            .thenReturn(TourClassificationSystemPage(listOf(classificationItem("자연 관광지")), 1001))
        `when`(tourApiClient.getClassificationSystemCodes(2, 1000))
            .thenReturn(TourClassificationSystemPage(listOf(classificationItem("문화 관광지", "A01010101")), 1001))
        `when`(repository.findAll()).thenReturn(listOf(existing))

        val count = service.sync()

        assertEquals(2, count)
        assertEquals("자연 관광지", existing.level3Name)
        verify(tourApiClient).getClassificationSystemCodes(2, 1000)
        verify(repository).saveAll(org.mockito.ArgumentMatchers.anyList())
    }

    private fun classificationItem(
        level3Name: String,
        level3Code: String = "A01010100",
    ) = TourClassificationSystemItem("A01", "자연", "A0101", "자연관광", level3Code, level3Name)

    private fun classificationEntity(level3Name: String) =
        TourClassificationCode(
            level1Code = "A01",
            level2Code = "A0101",
            level3Code = "A01010100",
            level1Name = "자연",
            level2Name = "자연관광",
            level3Name = level3Name,
        )
}
