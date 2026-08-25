package kr.hanchae.moyeotrip.client

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

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
}
