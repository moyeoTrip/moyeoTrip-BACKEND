package kr.hanchae.moyeotrip.service.weather

import kr.hanchae.moyeotrip.client.KmaWeatherForecast
import kr.hanchae.moyeotrip.client.KmaWeatherGrid
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.redisson.api.RBucket
import org.redisson.api.RLock
import org.redisson.api.RedissonClient
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

class RedissonGyeongbukWeatherForecastCacheTest {
    private val redissonClient = mock(RedissonClient::class.java)

    @Suppress("UNCHECKED_CAST")
    private val bucket = mock(RBucket::class.java) as RBucket<String>
    private val lock = mock(RLock::class.java)
    private val cache = RedissonGyeongbukWeatherForecastCache(redissonClient)
    private val baseDateTime = LocalDateTime.of(2026, 8, 25, 14, 30)
    private val grid = KmaWeatherGrid(102, 94)
    private val cacheKey = "MoyeoTrip:weather:forecast:gyeongbuk:202608251430:102:94"
    private val cacheValue = "2026-08-25T15:00\u001F27.5\u001F82\u001F4.2\u001F3.5\u001F3\u001F1"
    private val forecast =
        KmaWeatherForecast(
            forecastAt = LocalDateTime.of(2026, 8, 25, 15, 0),
            temperatureCelsius = 27.5,
            humidityPercent = 82,
            windSpeedMetersPerSecond = 4.2,
            precipitationMillimeters = 3.5,
            skyCode = 3,
            precipitationTypeCode = 1,
        )

    @Test
    fun `같은 기상청 발표시각과 격자의 Redis 캐시가 있으면 기상청을 호출하지 않는다`() {
        `when`(redissonClient.getBucket<String>(cacheKey)).thenReturn(bucket)
        `when`(bucket.get()).thenReturn(cacheValue)

        val result = cache.getOrLoad(baseDateTime, grid) { error("기상청을 호출하면 안 됩니다.") }

        assertEquals(forecast, result)
        verify(redissonClient, never()).getLock("$cacheKey:lock")
    }

    @Test
    fun `캐시가 없으면 락을 획득한 인스턴스가 다음 발표분 준비 시각까지 예보를 저장한다`() {
        `when`(redissonClient.getBucket<String>(cacheKey)).thenReturn(bucket)
        `when`(bucket.get()).thenReturn(null)
        `when`(redissonClient.getLock("$cacheKey:lock")).thenReturn(lock)
        `when`(lock.tryLock(5L, 30L, TimeUnit.SECONDS)).thenReturn(true)
        `when`(lock.isHeldByCurrentThread).thenReturn(true)

        val result = cache.getOrLoad(baseDateTime, grid) { forecast }

        assertEquals(forecast, result)
        verify(bucket).set(eq(cacheValue), any(Duration::class.java))
        verify(lock).unlock()
    }
}
