package kr.hanchae.moyeotrip.client

import kr.hanchae.moyeotrip.config.properties.WeatherApiProperties
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import java.time.LocalDateTime

class KmaWeatherApiClientTest {
    @Test
    fun `인증 키가 없으면 외부 요청 전에 거부한다`() {
        val client = KmaWeatherApiClient(RestClient.builder(), WeatherApiProperties(serviceKey = " "))

        assertThrows(IllegalStateException::class.java) {
            client.getUltraShortForecast(BASE_TIME, KmaWeatherGrid(91, 106))
        }
    }

    @Test
    fun `요청 파라미터를 구성하고 가장 이른 미래 예보의 단위를 변환한다`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val client = KmaWeatherApiClient(builder, WeatherApiProperties(serviceKey = " weather-key "))
        server
            .expect { request ->
                val query = request.uri.rawQuery
                assertTrue(request.uri.path.endsWith("/getUltraSrtFcst"))
                assertTrue(query.contains("authKey=weather-key"), query)
                assertTrue(query.contains("base_date=20260830"), query)
                assertTrue(query.contains("base_time=1200"), query)
                assertTrue(query.contains("nx=91"), query)
                assertTrue(query.contains("ny=106"), query)
                assertTrue(query.contains("numOfRows=11"), query)
            }.andRespond(withSuccess(successResponse("강수없음"), MediaType.APPLICATION_JSON))

        val result = client.getUltraShortForecast(BASE_TIME, KmaWeatherGrid(91, 106))!!

        assertEquals(LocalDateTime.of(2026, 8, 30, 13, 0), result.forecastAt)
        assertEquals(21.5, result.temperatureCelsius)
        assertEquals(73, result.humidityPercent)
        assertEquals(2.4, result.windSpeedMetersPerSecond)
        assertEquals(0.0, result.precipitationMillimeters)
        assertEquals(3, result.skyCode)
        assertEquals(0, result.precipitationTypeCode)
        server.verify()
    }

    @Test
    fun `강수량 문구와 숫자 단위를 변환한다`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val client = KmaWeatherApiClient(builder, WeatherApiProperties(serviceKey = "key"))
        server.expect { _ -> }.andRespond(withSuccess(successResponse("1mm 미만"), MediaType.APPLICATION_JSON))
        server.expect { _ -> }.andRespond(withSuccess(successResponse("2.5mm"), MediaType.APPLICATION_JSON))

        assertEquals(0.5, client.getUltraShortForecast(BASE_TIME, KmaWeatherGrid(91, 106))?.precipitationMillimeters)
        assertEquals(2.5, client.getUltraShortForecast(BASE_TIME, KmaWeatherGrid(91, 106))?.precipitationMillimeters)
        server.verify()
    }

    @Test
    fun `알 수 없는 숫자 값은 해당 관측값만 null로 둔다`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val client = KmaWeatherApiClient(builder, WeatherApiProperties(serviceKey = "key"))
        server.expect { _ -> }.andRespond(withSuccess(invalidValueResponse(), MediaType.APPLICATION_JSON))

        val result = client.getUltraShortForecast(BASE_TIME, KmaWeatherGrid(91, 106))!!

        assertNull(result.temperatureCelsius)
        assertNull(result.humidityPercent)
        assertNull(result.windSpeedMetersPerSecond)
        assertNull(result.precipitationMillimeters)
        assertNull(result.skyCode)
        assertNull(result.precipitationTypeCode)
    }

    @Test
    fun `응답 본문이 없거나 결과 코드가 실패면 예보가 없다`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val client = KmaWeatherApiClient(builder, WeatherApiProperties(serviceKey = "key"))
        server.expect { _ -> }.andRespond(withNoContent())
        server.expect { _ -> }.andRespond(withSuccess("""{"response":{"header":{"resultCode":"03"}}}""", MediaType.APPLICATION_JSON))

        assertNull(client.getUltraShortForecast(BASE_TIME, KmaWeatherGrid(91, 106)))
        assertNull(client.getUltraShortForecast(BASE_TIME, KmaWeatherGrid(91, 106)))
        server.verify()
    }

    @Test
    fun `예보 목록이 없거나 날짜가 잘못됐거나 모두 과거면 예보가 없다`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val client = KmaWeatherApiClient(builder, WeatherApiProperties(serviceKey = "key"))
        server
            .expect { _ ->
            }.andRespond(withSuccess("""{"response":{"header":{"resultCode":"00"},"body":null}}""", MediaType.APPLICATION_JSON))
        server
            .expect { _ ->
            }.andRespond(
                withSuccess("""{"response":{"header":{"resultCode":"00"},"body":{"items":{"item":null}}}}""", MediaType.APPLICATION_JSON),
            )
        server.expect { _ -> }.andRespond(withSuccess(noUsablePointResponse(), MediaType.APPLICATION_JSON))

        assertNull(client.getUltraShortForecast(BASE_TIME, KmaWeatherGrid(91, 106)))
        assertNull(client.getUltraShortForecast(BASE_TIME, KmaWeatherGrid(91, 106)))
        assertNull(client.getUltraShortForecast(BASE_TIME, KmaWeatherGrid(91, 106)))
        server.verify()
    }

    private fun successResponse(rain: String): String =
        """
        {
          "response": {
            "header": {"resultCode": "00"},
            "body": {"items": {"item": [
              {"category":"T1H","fcstDate":"20260830","fcstTime":"1100","fcstValue":"19.0"},
              {"category":"T1H","fcstDate":"20260830","fcstTime":"1400","fcstValue":"25.0"},
              {"category":"T1H","fcstDate":"20260830","fcstTime":"1300","fcstValue":"21.5"},
              {"category":"REH","fcstDate":"20260830","fcstTime":"1300","fcstValue":"73"},
              {"category":"WSD","fcstDate":"20260830","fcstTime":"1300","fcstValue":"2.4m/s"},
              {"category":"RN1","fcstDate":"20260830","fcstTime":"1300","fcstValue":"$rain"},
              {"category":"SKY","fcstDate":"20260830","fcstTime":"1300","fcstValue":"3"},
              {"category":"PTY","fcstDate":"20260830","fcstTime":"1300","fcstValue":"0"}
            ]}}
          }
        }
        """.trimIndent()

    private fun invalidValueResponse(): String =
        """
        {"response":{"header":{"resultCode":"00"},"body":{"items":{"item":[
          {"category":"T1H","fcstDate":"20260830","fcstTime":"1300","fcstValue":"-"},
          {"category":"REH","fcstDate":"20260830","fcstTime":"1300","fcstValue":"-"},
          {"category":"WSD","fcstDate":"20260830","fcstTime":"1300","fcstValue":"-"},
          {"category":"RN1","fcstDate":"20260830","fcstTime":"1300","fcstValue":"-"},
          {"category":"SKY","fcstDate":"20260830","fcstTime":"1300","fcstValue":"-"},
          {"category":"PTY","fcstDate":"20260830","fcstTime":"1300","fcstValue":"-"}
        ]}}}}
        """.trimIndent()

    private fun noUsablePointResponse(): String =
        """
        {"response":{"header":{"resultCode":"00"},"body":{"items":{"item":[
          {"category":"T1H","fcstDate":"invalid","fcstTime":"1300","fcstValue":"20"},
          {"category":"T1H","fcstDate":"20260830","fcstTime":"1100","fcstValue":"19"}
        ]}}}}
        """.trimIndent()

    companion object {
        private val BASE_TIME = LocalDateTime.of(2026, 8, 30, 12, 0)
    }
}
