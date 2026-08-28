package kr.hanchae.moyeotrip.service.tour

import kr.hanchae.moyeotrip.client.TourApiRateLimitExceededException
import kr.hanchae.moyeotrip.exception.BaseException
import kr.hanchae.moyeotrip.exception.ErrorCode
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
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

class TourismImageProxyServiceTest {
    @ParameterizedTest
    @ValueSource(
        strings = [
            "ftp://tong.visitkorea.or.kr/cms/resource/image.jpg",
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
    fun `공식 VisitKorea JPEG 이미지는 WebP로 변환한다`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val service = TourismImageProxyService(builder)
        val original = imageBytes("jpg")
        server
            .expect { request -> assertEquals("https", request.uri.scheme) }
            .andRespond(withSuccess(original, MediaType.IMAGE_JPEG))

        val result = service.getImage(OFFICIAL_IMAGE_URL)

        assertEquals(MediaType.parseMediaType("image/webp"), result.contentType)
        assertEquals(100, ByteArrayInputStream(result.bytes).use(ImageIO::read).width)
        server.verify()
    }

    @Test
    fun `공식 VisitKorea HTTP 이미지는 Object Storage 이관을 위해 조회할 수 있다`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val service = TourismImageProxyService(builder)
        server
            .expect { request -> assertEquals("http", request.uri.scheme) }
            .andRespond(withSuccess(imageBytes("jpg"), MediaType.IMAGE_JPEG))

        val result = service.getImage("http://tong.visitkorea.or.kr/cms/resource/image.jpg")

        assertEquals(MediaType.parseMediaType("image/webp"), result.contentType)
        server.verify()
    }

    @Test
    fun `관광공사 이미지 요청이 429이면 이관 작업을 재개할 수 있도록 요청 한도 예외를 던진다`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val service = TourismImageProxyService(builder)
        server.expect { _ -> }.andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS))

        assertThrows(TourApiRateLimitExceededException::class.java) { service.getImage(OFFICIAL_IMAGE_URL) }

        server.verify()
    }

    @Test
    fun `VisitKorea image jpg 응답도 WebP로 변환한다`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val service = TourismImageProxyService(builder)
        server.expect { _ -> }.andRespond(withSuccess(imageBytes("jpg"), MediaType.parseMediaType("image/jpg")))

        val result = service.getImage(OFFICIAL_IMAGE_URL)

        assertEquals(MediaType.parseMediaType("image/webp"), result.contentType)
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
    fun `공식 VisitKorea PNG 이미지는 WebP로 변환한다`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val service = TourismImageProxyService(builder)
        server.expect { _ -> }.andRespond(withSuccess(imageBytes("png"), MediaType.IMAGE_PNG))

        val result = service.getImage(OFFICIAL_IMAGE_URL)

        assertEquals(MediaType.parseMediaType("image/webp"), result.contentType)
        assertEquals(100, ByteArrayInputStream(result.bytes).use(ImageIO::read).width)
        server.verify()
    }

    @Test
    fun `공식 VisitKorea BMP 이미지는 WebP로 변환한다`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val service = TourismImageProxyService(builder)
        val bmp =
            ByteArrayOutputStream().use { output ->
                ImageIO.write(BufferedImage(100, 50, BufferedImage.TYPE_INT_RGB), "bmp", output)
                output.toByteArray()
            }
        server.expect { _ -> }.andRespond(withSuccess(bmp, MediaType.parseMediaType("image/bmp")))

        val result = service.getImage(OFFICIAL_IMAGE_URL)

        assertEquals(MediaType.parseMediaType("image/webp"), result.contentType)
        val converted = ByteArrayInputStream(result.bytes).use(ImageIO::read)
        assertEquals(100, converted.width)
        assertEquals(50, converted.height)
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

        private fun imageBytes(
            format: String,
            width: Int = 100,
            height: Int = 50,
        ): ByteArray =
            ByteArrayOutputStream().use { output ->
                ImageIO.write(BufferedImage(width, height, BufferedImage.TYPE_INT_RGB), format, output)
                output.toByteArray()
            }
    }
}
