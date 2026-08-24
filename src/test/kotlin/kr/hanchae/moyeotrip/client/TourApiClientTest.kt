package kr.hanchae.moyeotrip.client

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kr.hanchae.moyeotrip.config.properties.TourApiProperties
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
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

    companion object {
        private const val SUCCESS_RESPONSE =
            """{"response":{"header":{"resultCode":"0000","resultMsg":"OK"},"body":{"items":{"item":[{"contentid":"547853"}]},"numOfRows":1,"pageNo":1,"totalCount":1}}}"""
        private const val ERROR_RESPONSE =
            """{"OpenAPI_ServiceResponse":{"cmmMsgHeader":{"errMsg":"INVALID_REQUEST_PARAMETER_ERROR","returnReasonCode":"10"}}}"""
    }
}
