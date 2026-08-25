package kr.hanchae.moyeotrip.service.tour

import kr.hanchae.moyeotrip.exception.BaseException
import kr.hanchae.moyeotrip.exception.ErrorCode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.web.client.RestClient

class TourismImageProxyServiceTest {
    private val service = TourismImageProxyService(RestClient.builder())

    @ParameterizedTest
    @ValueSource(
        strings = [
            "http://tong.visitkorea.or.kr/cms/resource/image.jpg",
            "https://example.com/image.jpg",
            "https://visitkorea.or.kr.evil.example/image.jpg",
            "https://tong.visitkorea.or.kr:8443/cms/resource/image.jpg",
            "not-a-url",
        ],
    )
    fun `공식 VisitKorea HTTPS 주소가 아닌 이미지는 프록시하지 않는다`(imageUrl: String) {
        val exception = assertThrows(BaseException::class.java) { service.getImage(imageUrl) }

        assertEquals(ErrorCode.INVALID_TOURISM_IMAGE_URL, exception.errorCode)
    }
}
