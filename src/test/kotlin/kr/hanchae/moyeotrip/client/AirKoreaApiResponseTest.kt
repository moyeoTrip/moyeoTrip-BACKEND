package kr.hanchae.moyeotrip.client

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kr.hanchae.moyeotrip.config.properties.TourApiProperties
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient

class AirKoreaApiResponseTest {
    private val objectMapper = jacksonObjectMapper()

    @Test
    fun `시도별 실시간 측정정보 응답의 response body items를 역직렬화한다`() {
        val response =
            objectMapper.readValue<AirKoreaApiResponse>(
                """
                {
                  "response": {
                    "body": {
                      "totalCount": 53,
                      "items": [
                        {
                          "stationName": "청송읍",
                          "pm10Value": "13",
                          "pm25Value": "4",
                          "dataTime": "2026-08-25 14:00",
                          "khaiValue": "37"
                        }
                      ],
                      "pageNo": 1,
                      "numOfRows": 1
                    },
                    "header": {
                      "resultMsg": "NORMAL_CODE",
                      "resultCode": "00"
                    }
                  }
                }
                """.trimIndent(),
            )

        val body = requireNotNull(response.response.body)
        val item = body.items.single()

        assertEquals("00", response.response.header.resultCode)
        assertEquals(53, body.totalCount)
        assertEquals(1, body.numOfRows)
        assertEquals("청송읍", item.stationName)
        assertEquals("13", item.pm10Value)
        assertEquals("4", item.pm25Value)
    }

    @Test
    fun `경북 대기질 조회는 HttpExchange를 통해 기존 RestClient URI로 요청한다`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val client = AirKoreaApiClient(builder, TourApiProperties(tourApiKey = "abc+def%3D%3D"))
        server
            .expect { request ->
                val rawQuery = request.uri.rawQuery
                assertEquals("/B552584/ArpltnInforInqireSvc/getCtprvnRltmMesureDnsty", request.uri.path)
                assertEquals(true, rawQuery.contains("serviceKey=abc+def%3D%3D"), rawQuery)
                assertEquals(true, rawQuery.contains("numOfRows=1"), rawQuery)
                assertEquals(true, rawQuery.contains("pageNo=1"), rawQuery)
                assertEquals(true, rawQuery.contains("sidoName=%EA%B2%BD%EB%B6%81"), rawQuery)
            }.andRespond(withSuccess(SUCCESS_RESPONSE, MediaType.APPLICATION_JSON))

        val result = client.getGyeongbukAirQuality()

        assertEquals(AirQuality("청송읍", pm10 = 13, pm25 = 4), result)
        server.verify()
    }

    companion object {
        private const val SUCCESS_RESPONSE =
            """{"response":{"body":{"items":[{"stationName":"청송읍","pm10Value":"13","pm25Value":"4"}]},"header":{"resultCode":"00"}}}"""
    }
}
