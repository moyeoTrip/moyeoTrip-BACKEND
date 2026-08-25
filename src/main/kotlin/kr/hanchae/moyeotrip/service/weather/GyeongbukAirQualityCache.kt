package kr.hanchae.moyeotrip.service.weather

import kr.hanchae.moyeotrip.client.AirQuality
import org.redisson.api.RedissonClient
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

interface GyeongbukAirQualityCache {
    fun getOrLoad(
        date: LocalDate,
        loader: () -> AirQuality?,
    ): AirQuality?
}

@Component
class RedissonGyeongbukAirQualityCache(
    redissonClient: RedissonClient,
) : RedissonCacheSupport<AirQuality>(redissonClient, CACHE_NAME),
    GyeongbukAirQualityCache {
    override fun getOrLoad(
        date: LocalDate,
        loader: () -> AirQuality?,
    ): AirQuality? = super.getOrLoad(listOf(date), ttlUntilNextMidnight(), loader)

    private fun ttlUntilNextMidnight(): Duration {
        val now = ZonedDateTime.now(KOREA_ZONE_ID)
        return Duration.between(now, now.toLocalDate().plusDays(1).atStartOfDay(KOREA_ZONE_ID))
    }

    override fun serialize(value: AirQuality): String = value.toCacheValue()

    override fun deserialize(value: String): AirQuality? = value.toAirQualityOrNull()

    private fun AirQuality.toCacheValue(): String =
        listOf(stationName, pm10?.toString().orEmpty(), pm25?.toString().orEmpty()).joinToString(VALUE_SEPARATOR)

    private fun String.toAirQualityOrNull(): AirQuality? {
        val values = split(VALUE_SEPARATOR, limit = VALUE_COUNT)
        if (values.size != VALUE_COUNT || values.first().isBlank()) return null
        return AirQuality(
            stationName = values[0],
            pm10 = values[1].takeIf(String::isNotEmpty)?.toIntOrNull(),
            pm25 = values[2].takeIf(String::isNotEmpty)?.toIntOrNull(),
        )
    }

    companion object {
        private const val CACHE_NAME = "weather:air-quality:gyeongbuk"
        private const val VALUE_SEPARATOR = "\u001F"
        private const val VALUE_COUNT = 3
        private val KOREA_ZONE_ID: ZoneId = ZoneId.of("Asia/Seoul")
    }
}
