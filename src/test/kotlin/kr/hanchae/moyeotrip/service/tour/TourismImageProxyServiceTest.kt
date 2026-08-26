package kr.hanchae.moyeotrip.service.tour

import kr.hanchae.moyeotrip.exception.BaseException
import kr.hanchae.moyeotrip.exception.ErrorCode
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClient

class TourismImageProxyServiceTest {
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
        val service = TourismImageProxyService(RestClient.builder())

        val exception = assertThrows(BaseException::class.java) { service.getImage(imageUrl) }

        assertEquals(ErrorCode.INVALID_TOURISM_IMAGE_URL, exception.errorCode)
    }

    @Test
    fun `공식 VisitKorea JPEG 이미지는 바이너리로 반환한다`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val service = TourismImageProxyService(builder)
        val expected = byteArrayOf(1, 2, 3)
        server
            .expect { request -> assertEquals("https", request.uri.scheme) }
            .andRespond(withSuccess(expected, MediaType.IMAGE_JPEG))

        val result = service.getImage(OFFICIAL_IMAGE_URL)

        assertEquals(MediaType.IMAGE_JPEG, result.contentType)
        assertArrayEquals(expected, result.bytes)
        server.verify()
    }

    @Test
    fun `공식 이미지 요청도 성공 응답이 아니면 프록시하지 않는다`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val service = TourismImageProxyService(builder)
        server.expect { _ -> }.andRespond(withStatus(HttpStatus.NOT_FOUND))

        val exception = assertThrows(BaseException::class.java) { service.getImage(OFFICIAL_IMAGE_URL) }

        assertEquals(ErrorCode.TOURISM_IMAGE_FETCH_FAILED, exception.errorCode)
        server.verify()
    }

    @Test
    fun `JPEG가 아닌 응답은 프록시하지 않는다`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val service = TourismImageProxyService(builder)
        server.expect { _ -> }.andRespond(withSuccess("png", MediaType.IMAGE_PNG))

        val exception = assertThrows(BaseException::class.java) { service.getImage(OFFICIAL_IMAGE_URL) }

        assertEquals(ErrorCode.TOURISM_IMAGE_FETCH_FAILED, exception.errorCode)
        server.verify()
    }

    @Test
    fun `최대 크기를 초과한다고 응답한 이미지는 읽지 않고 거부한다`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val service = TourismImageProxyService(builder)
        server
            .expect { _ -> }
            .andRespond(
                withStatus(HttpStatus.OK)
                    .contentType(MediaType.IMAGE_JPEG)
                    .header("Content-Length", (20 * 1024 * 1024 + 1).toString())
                    .body("too-large"),
            )

        val exception = assertThrows(BaseException::class.java) { service.getImage(OFFICIAL_IMAGE_URL) }

        assertEquals(ErrorCode.TOURISM_IMAGE_FETCH_FAILED, exception.errorCode)
        server.verify()
    }

    @Test
    fun `이미지 원본 통신 오류는 외부 이미지 조회 오류로 변환한다`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val service = TourismImageProxyService(builder)
        server.expect { _ -> }.andRespond { throw ResourceAccessException("network unavailable") }

        val exception = assertThrows(BaseException::class.java) { service.getImage(OFFICIAL_IMAGE_URL) }

        assertEquals(ErrorCode.TOURISM_IMAGE_FETCH_FAILED, exception.errorCode)
        server.verify()
    }

    companion object {
        private const val OFFICIAL_IMAGE_URL = "https://tong.visitkorea.or.kr/cms/resource/image.jpg"
    }
}
