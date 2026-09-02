package kr.hanchae.moyeotrip.client

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kr.hanchae.moyeotrip.config.properties.TourApiProperties
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient

class TourApiClientTest {
    @Test
    fun `법정동 코드와 분류체계 코드를 페이지 정보와 함께 조회한다`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val client = TourApiClient(builder, jacksonObjectMapper(), TourApiProperties(tourApiKey = "test-key"))
        server
            .expect { request ->
                assertTrue(request.uri.path.endsWith("/ldongCode2"))
                assertTrue(request.uri.rawQuery.contains("pageNo=2"))
                assertTrue(request.uri.rawQuery.contains("numOfRows=30"))
                assertTrue(request.uri.rawQuery.contains("lDongRegnCd=47"))
            }.andRespond(withSuccess(LEGAL_DONG_RESPONSE, MediaType.APPLICATION_JSON))
        server
            .expect { request ->
                assertTrue(request.uri.path.endsWith("/lclsSystmCode2"))
                assertTrue(request.uri.rawQuery.contains("lclsSystmListYn=Y"))
            }.andRespond(withSuccess(CLASSIFICATION_RESPONSE, MediaType.APPLICATION_JSON))

        val legalDongs = client.getGyeongsangbukdoLegalDongCodes(2, 30)
        val classifications = client.getClassificationSystemCodes(1, 100)

        assertEquals(1, legalDongs.items.size)
        assertEquals(11, legalDongs.totalCount)
        assertEquals("관광지", classifications.items.single().lclsSystm1Nm)
        assertEquals(1, classifications.totalCount)
        server.verify()
    }

    @Test
    fun `법정동과 분류체계 응답에 items가 없으면 빈 목록을 반환한다`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val client = TourApiClient(builder, jacksonObjectMapper(), TourApiProperties(tourApiKey = "test-key"))
        server.expect { _ -> }.andRespond(withSuccess(LEGAL_DONG_EMPTY_RESPONSE, MediaType.APPLICATION_JSON))
        server.expect { _ -> }.andRespond(withSuccess(CLASSIFICATION_EMPTY_RESPONSE, MediaType.APPLICATION_JSON))

        assertTrue(client.getGyeongsangbukdoLegalDongCodes(1, 10).items.isEmpty())
        assertTrue(client.getClassificationSystemCodes(1, 10).items.isEmpty())
        server.verify()
    }

    @Test
    fun `법정동과 분류체계 API의 실패 결과 코드를 거부한다`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val client = TourApiClient(builder, jacksonObjectMapper(), TourApiProperties(tourApiKey = "test-key"))
        server.expect { _ -> }.andRespond(withSuccess(LEGAL_DONG_FAILURE_RESPONSE, MediaType.APPLICATION_JSON))
        server.expect { _ -> }.andRespond(withSuccess(CLASSIFICATION_FAILURE_RESPONSE, MediaType.APPLICATION_JSON))

        assertThrows(IllegalStateException::class.java) { client.getGyeongsangbukdoLegalDongCodes(1, 10) }
        assertThrows(IllegalStateException::class.java) { client.getClassificationSystemCodes(1, 10) }
        server.verify()
    }

    @Test
    fun `지역기반 관광정보 성공 응답과 빈 items를 처리한다`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val client = TourApiClient(builder, jacksonObjectMapper(), TourApiProperties(tourApiKey = "test-key"))
        server
            .expect { request ->
                assertTrue(request.uri.path.endsWith("/areaBasedList2"))
                assertTrue(request.uri.rawQuery.contains("contentTypeId=12"))
                assertTrue(request.uri.rawQuery.contains("arrange=C"))
            }.andRespond(withSuccess(AREA_RESPONSE, MediaType.APPLICATION_JSON))
        server.expect { _ -> }.andRespond(withSuccess(AREA_EMPTY_RESPONSE, MediaType.APPLICATION_JSON))

        val page = client.getAreaBasedTourismInformation(12, 1, 20)

        assertEquals("주왕산", page.items.single().title)
        assertEquals(1, page.totalCount)
        assertTrue(client.getAreaBasedTourismInformation(14, 1, 20).items.isEmpty())
        server.verify()
    }

    @Test
    fun `지역기반 관광정보의 공공데이터 오류와 실패 결과 코드를 거부한다`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val client = TourApiClient(builder, jacksonObjectMapper(), TourApiProperties(tourApiKey = "test-key"))
        server.expect { _ -> }.andRespond(withSuccess(ERROR_RESPONSE, MediaType.APPLICATION_JSON))
        server.expect { _ -> }.andRespond(withSuccess(AREA_FAILURE_RESPONSE, MediaType.APPLICATION_JSON))

        assertThrows(IllegalStateException::class.java) { client.getAreaBasedTourismInformation(12, 1, 20) }
        assertThrows(IllegalStateException::class.java) { client.getAreaBasedTourismInformation(12, 1, 20) }
        server.verify()
    }

    @Test
    fun `상세정보 요청은 8월 16일 방식대로 이미 인코딩된 서비스키를 그대로 사용한다`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val client = TourApiClient(builder, jacksonObjectMapper(), TourApiProperties(tourApiKey = "abc+def%3D%3D"))
        server
            .expect { request ->
                val rawQuery = request.uri.rawQuery
                assertTrue(rawQuery.contains("serviceKey=abc+def%3D%3D"), rawQuery)
                assertTrue(rawQuery.contains("contentId=547853"), rawQuery)
                assertTrue(rawQuery.contains("MobileOS=WEB"), rawQuery)
                assertTrue(rawQuery.contains("MobileApp=MoyeoTrip"), rawQuery)
                assertTrue(rawQuery.contains("_type=json"), rawQuery)
                assertFalse(rawQuery.contains("%253D"), rawQuery)
            }.andRespond(withSuccess(SUCCESS_RESPONSE, MediaType.APPLICATION_JSON))

        val result = client.getCommonDetail(547853L)

        assertEquals("547853", result?.contentid)
        server.verify()
    }

    @Test
    fun `400 응답도 예외로 버리지 않고 상태와 원문 JSON을 반환한다`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val client = TourApiClient(builder, jacksonObjectMapper(), TourApiProperties(tourApiKey = "abc+def=="))
        server
            .expect { request -> assertTrue(request.uri.rawQuery.contains("contentId=316103")) }
            .andRespond(
                withStatus(HttpStatus.BAD_REQUEST)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ERROR_RESPONSE),
            )

        val response = client.getCommonDetailRawJson(316103L)

        assertEquals(400, response.statusCode)
        assertEquals(ERROR_RESPONSE, response.body)
        server.verify()
    }

    @Test
    fun `공통 상세정보의 items가 빈 문자열이면 상세정보가 없는 것으로 처리한다`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val client = TourApiClient(builder, jacksonObjectMapper(), TourApiProperties(tourApiKey = "test-key"))
        server
            .expect { request -> assertTrue(request.uri.rawQuery.contains("contentId=2599344")) }
            .andRespond(withSuccess(EMPTY_ITEMS_RESPONSE, MediaType.APPLICATION_JSON))

        val result = client.getCommonDetail(2599344L)

        assertEquals(null, result)
        server.verify()
    }

    @Test
    fun `공통 상세정보는 HTTP 오류와 공공데이터 오류 및 실패 결과 코드를 거부한다`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val client = TourApiClient(builder, jacksonObjectMapper(), TourApiProperties(tourApiKey = "test-key"))
        server.expect { _ -> }.andRespond(withStatus(HttpStatus.BAD_REQUEST).body(ERROR_RESPONSE))
        server.expect { _ -> }.andRespond(withSuccess(ERROR_RESPONSE, MediaType.APPLICATION_JSON))
        server.expect { _ -> }.andRespond(withSuccess(COMMON_FAILURE_RESPONSE, MediaType.APPLICATION_JSON))

        assertThrows(IllegalStateException::class.java) { client.getCommonDetail(1L) }
        assertThrows(IllegalStateException::class.java) { client.getCommonDetail(2L) }
        assertThrows(IllegalStateException::class.java) { client.getCommonDetail(3L) }
        server.verify()
    }

    @Test
    fun `이미지정보의 items가 빈 문자열이면 빈 목록으로 처리한다`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val client = TourApiClient(builder, jacksonObjectMapper(), TourApiProperties(tourApiKey = "test-key"))
        server
            .expect { request ->
                assertTrue(request.uri.path.endsWith("/detailImage2"))
                assertTrue(request.uri.rawQuery.contains("contentId=2599344"))
                assertTrue(request.uri.rawQuery.contains("imageYN=Y"))
            }.andRespond(withSuccess(EMPTY_ITEMS_RESPONSE, MediaType.APPLICATION_JSON))

        val result = client.getImages(2599344L, "Y")

        assertTrue(result.isEmpty())
        server.verify()
    }

    @Test
    fun `이미지정보 조회가 429이면 요청 한도 예외로 변환한다`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val client = TourApiClient(builder, jacksonObjectMapper(), TourApiProperties(tourApiKey = "test-key"))
        server.expect { _ -> }.andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS))

        assertThrows(TourApiRateLimitExceededException::class.java) {
            client.getImages(2599344L, "Y")
        }

        server.verify()
    }

    @Test
    fun `이미지정보 성공 응답은 이미지 목록을 반환한다`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val client = TourApiClient(builder, jacksonObjectMapper(), TourApiProperties(tourApiKey = "test-key"))
        server.expect { _ -> }.andRespond(withSuccess(IMAGE_RESPONSE, MediaType.APPLICATION_JSON))

        val images = client.getImages(1L, "N")

        assertEquals("https://example.com/original.jpg", images.single().originimgurl)
        server.verify()
    }

    @Test
    fun `이미지정보의 공공데이터 오류와 실패 결과 코드를 거부한다`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val client = TourApiClient(builder, jacksonObjectMapper(), TourApiProperties(tourApiKey = "test-key"))
        server.expect { _ -> }.andRespond(withSuccess(ERROR_RESPONSE, MediaType.APPLICATION_JSON))
        server.expect { _ -> }.andRespond(withSuccess(IMAGE_FAILURE_RESPONSE, MediaType.APPLICATION_JSON))

        assertThrows(IllegalStateException::class.java) { client.getImages(1L, "Y") }
        assertThrows(IllegalStateException::class.java) { client.getImages(2L, "Y") }
        server.verify()
    }

    @Test
    fun `빈 TOUR API 키는 요청 URI를 만들 수 없다`() {
        val client = TourApiClient(RestClient.builder(), jacksonObjectMapper(), TourApiProperties(tourApiKey = "   "))

        assertThrows(IllegalArgumentException::class.java) {
            client.getGyeongsangbukdoLegalDongCodes(1, 10)
        }
    }

    @Test
    fun `공공데이터 오류 메시지는 제공된 문구를 조합하고 없으면 오류 코드로 대체한다`() {
        assertEquals(
            "INVALID: 잘못된 요청",
            OpenApiServiceErrorResponse(OpenApiErrorHeader("INVALID", "잘못된 요청", "10")).errorMessage(),
        )
        assertEquals(
            "권한 없음",
            OpenApiServiceErrorResponse(OpenApiErrorHeader(" ", "권한 없음", "20")).errorMessage(),
        )
        assertEquals(
            "공공데이터포털 요청에 실패했습니다. 오류 코드: 30",
            OpenApiServiceErrorResponse(OpenApiErrorHeader(returnReasonCode = "30")).errorMessage(),
        )
        assertEquals(
            "공공데이터포털 요청에 실패했습니다. 오류 코드: UNKNOWN",
            OpenApiServiceErrorResponse().errorMessage(),
        )
        assertNull(OpenApiServiceErrorResponse().cmmMsgHeader)
    }

    companion object {
        private const val SUCCESS_RESPONSE =
            """{"response":{"header":{"resultCode":"0000","resultMsg":"OK"},"body":{"items":{"item":[{"contentid":"547853"}]},"numOfRows":1,"pageNo":1,"totalCount":1}}}"""
        private const val ERROR_RESPONSE =
            """{"OpenAPI_ServiceResponse":{"cmmMsgHeader":{"errMsg":"INVALID_REQUEST_PARAMETER_ERROR","returnReasonCode":"10"}}}"""
        private const val EMPTY_ITEMS_RESPONSE =
            """{"response":{"header":{"resultCode":"0000","resultMsg":"OK"},"body":{"items":"","numOfRows":1,"pageNo":1,"totalCount":0}}}"""
        private const val LEGAL_DONG_RESPONSE =
            """{"response":{"header":{"resultCode":"0000","resultMsg":"OK"},"body":{"items":{"item":[{"lDongRegnCd":"47","lDongRegnNm":"경상북도","lDongSignguCd":"110","lDongSignguNm":"포항시"}]},"numOfRows":30,"pageNo":2,"totalCount":11}}}"""
        private const val LEGAL_DONG_EMPTY_RESPONSE =
            """{"response":{"header":{"resultCode":"0000","resultMsg":"OK"},"body":{"numOfRows":10,"pageNo":1,"totalCount":0}}}"""
        private const val LEGAL_DONG_FAILURE_RESPONSE =
            """{"response":{"header":{"resultCode":"9999","resultMsg":"FAIL"},"body":{"numOfRows":10,"pageNo":1,"totalCount":0}}}"""
        private const val CLASSIFICATION_RESPONSE =
            """{"response":{"header":{"resultCode":"0000","resultMsg":"OK"},"body":{"items":{"item":[{"lclsSystm1Cd":"NA","lclsSystm1Nm":"관광지","lclsSystm2Cd":"NA01","lclsSystm2Nm":"자연","lclsSystm3Cd":"NA010100","lclsSystm3Nm":"산"}]},"numOfRows":100,"pageNo":1,"totalCount":1}}}"""
        private const val CLASSIFICATION_EMPTY_RESPONSE =
            """{"response":{"header":{"resultCode":"0000","resultMsg":"OK"},"body":{"numOfRows":10,"pageNo":1,"totalCount":0}}}"""
        private const val CLASSIFICATION_FAILURE_RESPONSE =
            """{"response":{"header":{"resultCode":"9999","resultMsg":"FAIL"},"body":{"numOfRows":10,"pageNo":1,"totalCount":0}}}"""
        private const val AREA_RESPONSE =
            """{"response":{"header":{"resultCode":"0000","resultMsg":"OK"},"body":{"items":{"item":[{"contentid":"1","contenttypeid":"12","title":"주왕산"}]},"numOfRows":20,"pageNo":1,"totalCount":1}}}"""
        private const val AREA_EMPTY_RESPONSE =
            """{"response":{"header":{"resultCode":"0000","resultMsg":"OK"},"body":{"numOfRows":20,"pageNo":1,"totalCount":0}}}"""
        private const val AREA_FAILURE_RESPONSE =
            """{"response":{"header":{"resultCode":"9999","resultMsg":"FAIL"},"body":{"numOfRows":20,"pageNo":1,"totalCount":0}}}"""
        private const val COMMON_FAILURE_RESPONSE =
            """{"response":{"header":{"resultCode":"9999","resultMsg":"FAIL"},"body":{"items":null,"numOfRows":1,"pageNo":1,"totalCount":0}}}"""
        private const val IMAGE_RESPONSE =
            """{"response":{"header":{"resultCode":"0000","resultMsg":"OK"},"body":{"items":{"item":[{"contentid":"1","imgname":"원본","originimgurl":"https://example.com/original.jpg","serialnum":"1","cpyrhtDivCd":"Type1"}]},"numOfRows":1000,"pageNo":1,"totalCount":1}}}"""
        private const val IMAGE_FAILURE_RESPONSE =
            """{"response":{"header":{"resultCode":"9999","resultMsg":"FAIL"},"body":{"items":null,"numOfRows":1000,"pageNo":1,"totalCount":0}}}"""
    }
}
