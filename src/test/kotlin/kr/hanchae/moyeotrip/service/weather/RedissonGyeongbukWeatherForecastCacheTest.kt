package kr.hanchae.moyeotrip.service.weather

import kr.hanchae.moyeotrip.client.KmaWeatherForecast
import kr.hanchae.moyeotrip.client.KmaWeatherGrid
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
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

    @Test
    fun `선택 기상값이 비어 있는 Redis 캐시는 null 필드 예보로 복원한다`() {
        `when`(redissonClient.getBucket<String>(cacheKey)).thenReturn(bucket)
        `when`(bucket.get()).thenReturn("2026-08-25T15:00")

        val result = cache.getOrLoad(baseDateTime, grid) { error("기상청을 호출하면 안 됩니다.") }

        assertEquals(LocalDateTime.of(2026, 8, 25, 15, 0), result?.forecastAt)
        assertNull(result?.temperatureCelsius)
        assertNull(result?.humidityPercent)
        assertNull(result?.windSpeedMetersPerSecond)
        assertNull(result?.precipitationMillimeters)
        assertNull(result?.skyCode)
        assertNull(result?.precipitationTypeCode)
    }

    @Test
    fun `형식이 잘못된 Redis 예보는 무시하고 원본을 조회한다`() {
        `when`(redissonClient.getBucket<String>(cacheKey)).thenReturn(bucket)
        `when`(bucket.get()).thenReturn("필드가부족함")
        `when`(redissonClient.getLock("$cacheKey:lock")).thenReturn(lock)
        `when`(lock.tryLock(5L, 30L, TimeUnit.SECONDS)).thenReturn(true)
        `when`(lock.isHeldByCurrentThread).thenReturn(true)

        val result = cache.getOrLoad(baseDateTime, grid) { forecast }

        assertEquals(forecast, result)
        verify(lock).unlock()
    }

    @Test
    fun `날짜를 해석할 수 없는 Redis 예보도 무시한다`() {
        `when`(redissonClient.getBucket<String>(cacheKey)).thenReturn(bucket)
        `when`(bucket.get()).thenReturn("잘못된 날짜27.5824.23.531")
        `when`(redissonClient.getLock("$cacheKey:lock")).thenReturn(lock)
        `when`(lock.tryLock(5L, 30L, TimeUnit.SECONDS)).thenReturn(false)

        val result = cache.getOrLoad(baseDateTime, grid) { forecast }

        assertEquals(forecast, result)
        verify(bucket, never()).set(any(), any(Duration::class.java))
    }

    @Test
    fun `선택 기상값이 null인 원본 예보는 빈 필드로 캐싱한다`() {
        val nullForecast = KmaWeatherForecast(LocalDateTime.of(2026, 8, 25, 15, 0), null, null, null, null, null, null)
        `when`(redissonClient.getBucket<String>(cacheKey)).thenReturn(bucket)
        `when`(bucket.get()).thenReturn(null)
        `when`(redissonClient.getLock("$cacheKey:lock")).thenReturn(lock)
        `when`(lock.tryLock(5L, 30L, TimeUnit.SECONDS)).thenReturn(true)
        `when`(lock.isHeldByCurrentThread).thenReturn(true)

        val result = cache.getOrLoad(baseDateTime, grid) { nullForecast }

        assertEquals(nullForecast, result)
        verify(bucket).set(eq("2026-08-25T15:00"), any(Duration::class.java))
    }
}
