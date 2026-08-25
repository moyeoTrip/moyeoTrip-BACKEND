package kr.hanchae.moyeotrip.service.tour

import kr.hanchae.moyeotrip.config.properties.TourApiProperties
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.springframework.boot.ApplicationArguments

class LegalDongCodeStartupSyncTest {
    private val legalDongService = mock(LegalDongCodeSyncService::class.java)
    private val classificationService = mock(TourClassificationCodeSyncService::class.java)
    private val contentService = mock(TourismContentSyncService::class.java)

    @Test
    fun `시작 동기화 설정이 꺼져 있으면 API를 호출하지 않는다`() {
        val scheduler = startupSync(TourApiProperties("api-key", syncOnStartup = false))

        scheduler.run(mock(ApplicationArguments::class.java))

        verifyNoInteractions(legalDongService, classificationService, contentService)
    }

    @Test
    fun `API 키가 공백이면 월간 동기화를 건너뛴다`() {
        val scheduler = startupSync(TourApiProperties("   ", syncOnStartup = true))

        scheduler.syncMonthly()

        verifyNoInteractions(legalDongService, classificationService, contentService)
    }

    @Test
    fun `API 키가 있으면 세 종류 데이터를 순서대로 동기화한다`() {
        val scheduler = startupSync(TourApiProperties("api-key", syncOnStartup = true))
        `when`(legalDongService.syncGyeongsangbukdo()).thenReturn(22)
        `when`(classificationService.sync()).thenReturn(100)
        `when`(contentService.syncGyeongsangbukdo()).thenReturn(3100)

        scheduler.run(mock(ApplicationArguments::class.java))

        verify(legalDongService).syncGyeongsangbukdo()
        verify(classificationService).sync()
        verify(contentService).syncGyeongsangbukdo()
    }

    private fun startupSync(properties: TourApiProperties) =
        LegalDongCodeStartupSync(properties, legalDongService, classificationService, contentService)
}
