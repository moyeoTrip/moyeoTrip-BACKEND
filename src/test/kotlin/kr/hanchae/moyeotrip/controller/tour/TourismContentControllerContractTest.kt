package kr.hanchae.moyeotrip.controller.tour

import kr.hanchae.moyeotrip.service.tour.TourismContentService
import kr.hanchae.moyeotrip.service.tour.TourismImageBinary
import kr.hanchae.moyeotrip.service.tour.TourismImageProxyService
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.http.MediaType

class TourismContentControllerContractTest {
    private val tourismContentService = mock(TourismContentService::class.java)
    private val tourismImageProxyService = mock(TourismImageProxyService::class.java)
    private val controller = TourismContentController(tourismContentService, tourismImageProxyService)

    @Test
    fun `관광 이미지 프록시는 원본 이미지 타입과 바이트 및 캐시 헤더를 반환한다`() {
        val imageUrl = "https://tong.visitkorea.or.kr/cms/resource/image.jpg"
        val bytes = byteArrayOf(1, 2, 3)
        `when`(tourismImageProxyService.getImage(imageUrl)).thenReturn(TourismImageBinary(MediaType.IMAGE_JPEG, bytes))

        val response = controller.getImage(imageUrl)

        assertEquals(MediaType.IMAGE_JPEG, response.headers.contentType)
        assertEquals("max-age=43200, public", response.headers.cacheControl)
        assertArrayEquals(bytes, response.body)
        verify(tourismImageProxyService).getImage(imageUrl)
    }
}
