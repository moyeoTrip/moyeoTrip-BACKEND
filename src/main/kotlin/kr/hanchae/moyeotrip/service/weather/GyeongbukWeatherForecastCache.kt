package kr.hanchae.moyeotrip.service.weather

import kr.hanchae.moyeotrip.client.KmaWeatherForecast
import kr.hanchae.moyeotrip.client.KmaWeatherGrid
import org.redisson.api.RedissonClient
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

interface GyeongbukWeatherForecastCache {
    fun getOrLoad(
        baseDateTime: LocalDateTime,
        grid: KmaWeatherGrid,
        loader: () -> KmaWeatherForecast?,
    ): KmaWeatherForecast?
}

@Component
class RedissonGyeongbukWeatherForecastCache(
    redissonClient: RedissonClient,
) : RedissonCacheSupport<KmaWeatherForecast>(redissonClient, CACHE_NAME),
    GyeongbukWeatherForecastCache {
    override fun getOrLoad(
        baseDateTime: LocalDateTime,
        grid: KmaWeatherGrid,
        loader: () -> KmaWeatherForecast?,
    ): KmaWeatherForecast? =
        super.getOrLoad(
            keyParts = listOf(baseDateTime.format(CACHE_KEY_DATE_TIME_FORMAT), grid.x, grid.y),
            ttl = ttlUntilNextPublication(baseDateTime),
            loader = loader,
        )

    private fun ttlUntilNextPublication(baseDateTime: LocalDateTime): Duration {
        val now = ZonedDateTime.now(KOREA_ZONE_ID)
        val nextPublicationReadyAt = baseDateTime.atZone(KOREA_ZONE_ID).plusHours(1).plusMinutes(15)
        return Duration.between(now, nextPublicationReadyAt).takeIf { !it.isNegative && !it.isZero } ?: MINIMUM_CACHE_TTL
    }

    override fun serialize(value: KmaWeatherForecast): String = value.toCacheValue()

    override fun deserialize(value: String): KmaWeatherForecast? = value.toKmaWeatherForecastOrNull()

    private fun KmaWeatherForecast.toCacheValue(): String =
        listOf(
            forecastAt.toString(),
            temperatureCelsius?.toString().orEmpty(),
            humidityPercent?.toString().orEmpty(),
            windSpeedMetersPerSecond?.toString().orEmpty(),
            precipitationMillimeters?.toString().orEmpty(),
            skyCode?.toString().orEmpty(),
            precipitationTypeCode?.toString().orEmpty(),
        ).joinToString(VALUE_SEPARATOR)

    private fun String.toKmaWeatherForecastOrNull(): KmaWeatherForecast? {
        val values = split(VALUE_SEPARATOR, limit = VALUE_COUNT)
        if (values.size != VALUE_COUNT) return null
        return runCatching {
            KmaWeatherForecast(
                forecastAt = LocalDateTime.parse(values[0]),
                temperatureCelsius = values[1].toDoubleOrNull(),
                humidityPercent = values[2].toIntOrNull(),
                windSpeedMetersPerSecond = values[3].toDoubleOrNull(),
                precipitationMillimeters = values[4].toDoubleOrNull(),
                skyCode = values[5].toIntOrNull(),
                precipitationTypeCode = values[6].toIntOrNull(),
            )
        }.getOrNull()
    }

    companion object {
        private const val CACHE_NAME = "weather:forecast:gyeongbuk"
        private const val VALUE_SEPARATOR = "\u001F"
        private const val VALUE_COUNT = 7
        private val CACHE_KEY_DATE_TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmm")
        private val KOREA_ZONE_ID: ZoneId = ZoneId.of("Asia/Seoul")
        private val MINIMUM_CACHE_TTL: Duration = Duration.ofMinutes(1)
    }
}
