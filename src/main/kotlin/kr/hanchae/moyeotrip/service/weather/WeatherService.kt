package kr.hanchae.moyeotrip.service.weather

import kr.hanchae.moyeotrip.client.AirQuality
import kr.hanchae.moyeotrip.client.AirQualityClient
import kr.hanchae.moyeotrip.client.KmaWeatherClient
import kr.hanchae.moyeotrip.client.KmaWeatherForecast
import kr.hanchae.moyeotrip.client.KmaWeatherGrid
import kr.hanchae.moyeotrip.config.properties.WeatherApiProperties
import kr.hanchae.moyeotrip.controller.weather.response.GyeongbukWeatherCondition
import kr.hanchae.moyeotrip.controller.weather.response.GyeongbukWeatherResponse
import kr.hanchae.moyeotrip.exception.BaseException
import kr.hanchae.moyeotrip.exception.ErrorCode
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class WeatherService(
    private val kmaWeatherClient: KmaWeatherClient,
    private val airQualityClient: AirQualityClient,
    private val properties: WeatherApiProperties,
    private val airQualityCache: GyeongbukAirQualityCache,
    private val weatherForecastCache: GyeongbukWeatherForecastCache,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun getGyeongbukWeather(now: LocalDateTime = LocalDateTime.now()): GyeongbukWeatherResponse {
        val baseDateTime = now.toKmaPublicationDateTime()
        val primaryForecast = getForecast(baseDateTime, properties.primaryGrid())
        val fallbackApplied = primaryForecast == null
        val forecast =
            primaryForecast
                ?: getForecast(baseDateTime, properties.fallbackGrid())
                ?: throw BaseException(ErrorCode.WEATHER_DATA_UNAVAILABLE)
        val airQuality =
            runCatching {
                airQualityCache.getOrLoad(now.toLocalDate()) { airQualityClient.getGyeongbukAirQuality() }
            }.onFailure { exception ->
                log.warn("경상북도 미세먼지 조회에 실패했습니다. 날씨 정보만 반환합니다.", exception)
            }.getOrNull()
        return GyeongbukWeatherResponse(
            condition = forecast.classify(airQuality),
            locationName = if (fallbackApplied) properties.fallbackLocationName else properties.primaryLocationName,
            fallbackApplied = fallbackApplied,
            forecastAt = forecast.forecastAt,
            temperatureCelsius = forecast.temperatureCelsius,
            humidityPercent = forecast.humidityPercent,
            windSpeedMetersPerSecond = forecast.windSpeedMetersPerSecond,
            precipitationMillimeters = forecast.precipitationMillimeters,
            pm10 = airQuality?.pm10,
            pm25 = airQuality?.pm25,
        )
    }

    private fun LocalDateTime.toKmaPublicationDateTime(): LocalDateTime {
        val currentHour = withMinute(0).withSecond(0).withNano(0)
        return if (minute < KMA_FORECAST_READY_MINUTE) currentHour.minusHours(1).withMinute(30) else currentHour.withMinute(30)
    }

    private fun WeatherApiProperties.primaryGrid() = KmaWeatherGrid(primaryGridX, primaryGridY)

    private fun WeatherApiProperties.fallbackGrid() = KmaWeatherGrid(fallbackGridX, fallbackGridY)

    private fun getForecast(
        baseDateTime: LocalDateTime,
        grid: KmaWeatherGrid,
    ): KmaWeatherForecast? =
        runCatching {
            weatherForecastCache.getOrLoad(baseDateTime, grid) {
                kmaWeatherClient.getUltraShortForecast(baseDateTime, grid)
            }
        }.getOrNull()

    private fun KmaWeatherForecast.classify(airQuality: AirQuality?): GyeongbukWeatherCondition =
        when {
            precipitationMillimeters != null && precipitationMillimeters >= HEAVY_RAIN_PER_HOUR_MILLIMETERS ->
                GyeongbukWeatherCondition.HEAVY_RAIN

            windSpeedMetersPerSecond != null && windSpeedMetersPerSecond >= STRONG_WIND_METERS_PER_SECOND ->
                GyeongbukWeatherCondition.STRONG_WIND

            temperatureCelsius != null && temperatureCelsius >= HEAT_WAVE_CELSIUS -> GyeongbukWeatherCondition.HEAT_WAVE
            precipitationTypeCode in SNOW_PRECIPITATION_TYPES -> GyeongbukWeatherCondition.SNOW
            precipitationTypeCode in RAIN_PRECIPITATION_TYPES -> GyeongbukWeatherCondition.RAIN
            humidityPercent != null &&
                humidityPercent >= FOG_HUMIDITY_PERCENT &&
                windSpeedMetersPerSecond != null &&
                windSpeedMetersPerSecond < FOG_MAX_WIND_METERS_PER_SECOND &&
                (precipitationMillimeters ?: 0.0) == 0.0 ->
                GyeongbukWeatherCondition.FOG

            airQuality.isFineDustBad() -> GyeongbukWeatherCondition.FINE_DUST
            skyCode in CLOUDY_SKY_CODES -> GyeongbukWeatherCondition.CLOUDY
            else -> GyeongbukWeatherCondition.SUNNY
        }

    private fun AirQuality?.isFineDustBad(): Boolean =
        this != null && (pm10 != null && pm10 >= BAD_PM10 || pm25 != null && pm25 >= BAD_PM25)

    companion object {
        private const val KMA_FORECAST_READY_MINUTE = 45
        private const val HEAVY_RAIN_PER_HOUR_MILLIMETERS = 30.0
        private const val STRONG_WIND_METERS_PER_SECOND = 14.0
        private const val HEAT_WAVE_CELSIUS = 33.0
        private const val FOG_HUMIDITY_PERCENT = 95
        private const val FOG_MAX_WIND_METERS_PER_SECOND = 1.5
        private const val BAD_PM10 = 81
        private const val BAD_PM25 = 36
        private val SNOW_PRECIPITATION_TYPES = setOf(3, 7)
        private val RAIN_PRECIPITATION_TYPES = setOf(1, 2, 4, 5, 6)
        private val CLOUDY_SKY_CODES = setOf(3, 4)
    }
}
