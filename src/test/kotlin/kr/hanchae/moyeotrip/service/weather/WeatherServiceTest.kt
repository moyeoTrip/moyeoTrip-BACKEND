package kr.hanchae.moyeotrip.service.weather

import kr.hanchae.moyeotrip.client.AirQuality
import kr.hanchae.moyeotrip.client.AirQualityClient
import kr.hanchae.moyeotrip.client.KmaWeatherClient
import kr.hanchae.moyeotrip.client.KmaWeatherForecast
import kr.hanchae.moyeotrip.client.KmaWeatherGrid
import kr.hanchae.moyeotrip.config.properties.WeatherApiProperties
import kr.hanchae.moyeotrip.controller.weather.response.GyeongbukWeatherCondition
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.time.LocalDate
import java.time.LocalDateTime

class WeatherServiceTest {
    private val kmaWeatherClient = mock(KmaWeatherClient::class.java)
    private val airQualityClient = mock(AirQualityClient::class.java)
    private val properties = WeatherApiProperties()
    private val airQualityCache =
        object : GyeongbukAirQualityCache {
            override fun getOrLoad(
                date: LocalDate,
                loader: () -> AirQuality?,
            ): AirQuality? = loader()
        }
    private val weatherForecastCache =
        object : GyeongbukWeatherForecastCache {
            override fun getOrLoad(
                baseDateTime: LocalDateTime,
                grid: KmaWeatherGrid,
                loader: () -> KmaWeatherForecast?,
            ): KmaWeatherForecast? = loader()
        }
    private val service = WeatherService(kmaWeatherClient, airQualityClient, properties, airQualityCache, weatherForecastCache)
    private val now = LocalDateTime.of(2026, 8, 25, 14, 50)

    @Nested
    inner class `날씨 상태 분류` {
        @Test
        fun `폭우를 다른 상태보다 우선해 반환한다`() {
            stubPrimary(forecast(precipitationMillimeters = 30.0, windSpeedMetersPerSecond = 20.0, temperatureCelsius = 35.0))

            val response = service.getGyeongbukWeather(now)

            assertEquals(GyeongbukWeatherCondition.HEAVY_RAIN, response.condition)
        }

        @Test
        fun `강풍과 폭염을 각각 기준값부터 반환한다`() {
            stubPrimary(forecast(windSpeedMetersPerSecond = 14.0))
            assertEquals(GyeongbukWeatherCondition.STRONG_WIND, service.getGyeongbukWeather(now).condition)

            stubPrimary(forecast(temperatureCelsius = 33.0))
            assertEquals(GyeongbukWeatherCondition.HEAT_WAVE, service.getGyeongbukWeather(now).condition)
        }

        @Test
        fun `눈 비 안개를 각각 반환한다`() {
            stubPrimary(forecast(precipitationTypeCode = 3))
            assertEquals(GyeongbukWeatherCondition.SNOW, service.getGyeongbukWeather(now).condition)

            stubPrimary(forecast(precipitationTypeCode = 1))
            assertEquals(GyeongbukWeatherCondition.RAIN, service.getGyeongbukWeather(now).condition)

            stubPrimary(forecast(humidityPercent = 95, windSpeedMetersPerSecond = 1.4))
            assertEquals(GyeongbukWeatherCondition.FOG, service.getGyeongbukWeather(now).condition)
        }

        @Test
        fun `미세먼지 구름 맑음을 각각 반환한다`() {
            stubPrimary(forecast())
            `when`(airQualityClient.getGyeongbukAirQuality()).thenReturn(AirQuality("안동", pm10 = 81, pm25 = 10))
            assertEquals(GyeongbukWeatherCondition.FINE_DUST, service.getGyeongbukWeather(now).condition)

            val nextDay = now.plusDays(1)
            stubPrimary(forecast(skyCode = 3), nextDay)
            `when`(airQualityClient.getGyeongbukAirQuality()).thenReturn(null)
            assertEquals(GyeongbukWeatherCondition.CLOUDY, service.getGyeongbukWeather(nextDay).condition)

            val dayAfterNext = now.plusDays(2)
            stubPrimary(forecast(), dayAfterNext)
            assertEquals(GyeongbukWeatherCondition.SUNNY, service.getGyeongbukWeather(dayAfterNext).condition)
        }
    }

    @Test
    fun `포항 조회에 실패하면 안동 대표 격자를 조회해 응답한다`() {
        `when`(
            kmaWeatherClient.getUltraShortForecast(
                baseDateTime(),
                KmaWeatherGrid(102, 94),
            ),
        ).thenThrow(IllegalStateException("temporary KMA failure"))
        `when`(
            kmaWeatherClient.getUltraShortForecast(
                baseDateTime(),
                KmaWeatherGrid(91, 106),
            ),
        ).thenReturn(forecast())

        val response = service.getGyeongbukWeather(now)

        assertTrue(response.fallbackApplied)
        assertEquals("경상북도 안동시", response.locationName)
        verify(kmaWeatherClient).getUltraShortForecast(
            baseDateTime(),
            KmaWeatherGrid(91, 106),
        )
    }

    @Test
    fun `포항 조회에 성공하면 대체 지역을 사용하지 않는다`() {
        stubPrimary(forecast())

        val response = service.getGyeongbukWeather(now)

        assertFalse(response.fallbackApplied)
        assertEquals("경상북도 포항시", response.locationName)
    }

    private fun stubPrimary(
        forecast: KmaWeatherForecast,
        at: LocalDateTime = now,
    ) {
        `when`(
            kmaWeatherClient.getUltraShortForecast(
                baseDateTime(at),
                KmaWeatherGrid(102, 94),
            ),
        ).thenReturn(forecast)
        `when`(airQualityClient.getGyeongbukAirQuality()).thenReturn(null)
    }

    private fun baseDateTime(at: LocalDateTime = now): LocalDateTime =
        at
            .withMinute(30)
            .withSecond(0)
            .withNano(0)

    private fun forecast(
        temperatureCelsius: Double? = 25.0,
        humidityPercent: Int? = 60,
        windSpeedMetersPerSecond: Double? = 3.0,
        precipitationMillimeters: Double? = 0.0,
        skyCode: Int? = 1,
        precipitationTypeCode: Int? = 0,
    ): KmaWeatherForecast =
        KmaWeatherForecast(
            forecastAt = now.withMinute(0).withSecond(0).withNano(0),
            temperatureCelsius = temperatureCelsius,
            humidityPercent = humidityPercent,
            windSpeedMetersPerSecond = windSpeedMetersPerSecond,
            precipitationMillimeters = precipitationMillimeters,
            skyCode = skyCode,
            precipitationTypeCode = precipitationTypeCode,
        )
}
