package kr.hanchae.moyeotrip.service.weather

import kr.hanchae.moyeotrip.client.AirQuality
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.redisson.api.RBucket
import org.redisson.api.RLock
import org.redisson.api.RedissonClient
import java.time.Duration
import java.time.LocalDate
import java.util.concurrent.TimeUnit

class RedissonGyeongbukAirQualityCacheTest {
    private val redissonClient = mock(RedissonClient::class.java)

    @Suppress("UNCHECKED_CAST")
    private val bucket = mock(RBucket::class.java) as RBucket<String>
    private val lock = mock(RLock::class.java)
    private val cache = RedissonGyeongbukAirQualityCache(redissonClient)
    private val date = LocalDate.of(2026, 8, 25)
    private val cacheKey = "MoyeoTrip:weather:air-quality:gyeongbuk:2026-08-25"

    @Test
    fun `같은 날짜의 Redis 캐시가 있으면 AirKorea를 호출하지 않는다`() {
        `when`(redissonClient.getBucket<String>(cacheKey)).thenReturn(bucket)
        `when`(bucket.get()).thenReturn("청송읍\u001F13\u001F4")

        val result = cache.getOrLoad(date) { error("AirKorea를 호출하면 안 됩니다.") }

        assertEquals(AirQuality("청송읍", 13, 4), result)
        verify(redissonClient, never()).getLock("$cacheKey:lock")
    }

    @Test
    fun `캐시가 없으면 락을 획득한 인스턴스가 조회 결과를 다음 자정까지 저장한다`() {
        `when`(redissonClient.getBucket<String>(cacheKey)).thenReturn(bucket)
        `when`(bucket.get()).thenReturn(null)
        `when`(redissonClient.getLock("$cacheKey:lock")).thenReturn(lock)
        `when`(lock.tryLock(5L, 30L, TimeUnit.SECONDS)).thenReturn(true)
        `when`(lock.isHeldByCurrentThread).thenReturn(true)

        val result = cache.getOrLoad(date) { AirQuality("청송읍", 13, 4) }

        assertEquals(AirQuality("청송읍", 13, 4), result)
        verify(bucket).set(eq("청송읍\u001F13\u001F4"), any(Duration::class.java))
        verify(lock).unlock()
    }

    @Test
    fun `Redis 버킷을 가져오지 못하면 원본 조회 결과를 그대로 반환한다`() {
        `when`(redissonClient.getBucket<String>(cacheKey)).thenThrow(IllegalStateException("Redis unavailable"))
        val expected = AirQuality("청송읍", 13, 4)

        val result = cache.getOrLoad(date) { expected }

        assertEquals(expected, result)
        verify(redissonClient, never()).getLock("$cacheKey:lock")
    }

    @Test
    fun `락을 얻지 못한 경우 다른 인스턴스가 채운 캐시를 반환한다`() {
        `when`(redissonClient.getBucket<String>(cacheKey)).thenReturn(bucket)
        `when`(bucket.get()).thenReturn(null, "청송읍\u001F13\u001F4")
        `when`(redissonClient.getLock("$cacheKey:lock")).thenReturn(lock)
        `when`(lock.tryLock(5L, 30L, TimeUnit.SECONDS)).thenReturn(false)

        val result = cache.getOrLoad(date) { error("다른 인스턴스의 캐시가 있어 원본 조회하면 안 됩니다.") }

        assertEquals(AirQuality("청송읍", 13, 4), result)
        verify(bucket, never()).set(any(), any(Duration::class.java))
        verify(lock, never()).unlock()
    }

    @Test
    fun `Redis 락 생성이나 획득에 실패해도 원본 조회를 반환한다`() {
        `when`(redissonClient.getBucket<String>(cacheKey)).thenReturn(bucket)
        `when`(bucket.get()).thenReturn(null)
        `when`(redissonClient.getLock("$cacheKey:lock")).thenReturn(lock)
        `when`(lock.tryLock(5L, 30L, TimeUnit.SECONDS)).thenThrow(IllegalStateException("lock unavailable"))
        val expected = AirQuality("청송읍", 13, 4)

        val result = cache.getOrLoad(date) { expected }

        assertEquals(expected, result)
        verify(bucket, never()).set(any(), any(Duration::class.java))
    }

    @Test
    fun `원본 조회 결과가 없으면 빈 값을 캐싱하지 않는다`() {
        `when`(redissonClient.getBucket<String>(cacheKey)).thenReturn(bucket)
        `when`(bucket.get()).thenReturn(null)
        `when`(redissonClient.getLock("$cacheKey:lock")).thenReturn(lock)
        `when`(lock.tryLock(5L, 30L, TimeUnit.SECONDS)).thenReturn(true)
        `when`(lock.isHeldByCurrentThread).thenReturn(false)

        val result = cache.getOrLoad(date) { null }

        assertNull(result)
        verify(bucket, never()).set(any(), any(Duration::class.java))
        verify(lock, never()).unlock()
    }

    @Test
    fun `캐시 저장에 실패해도 원본 조회 결과는 반환하고 락을 해제한다`() {
        `when`(redissonClient.getBucket<String>(cacheKey)).thenReturn(bucket)
        `when`(bucket.get()).thenReturn(null)
        `when`(redissonClient.getLock("$cacheKey:lock")).thenReturn(lock)
        `when`(lock.tryLock(5L, 30L, TimeUnit.SECONDS)).thenReturn(true)
        `when`(lock.isHeldByCurrentThread).thenReturn(true)
        doThrow(IllegalStateException("write unavailable"))
            .`when`(bucket)
            .set(any(), any(Duration::class.java))
        val expected = AirQuality("청송읍", 13, 4)

        val result = cache.getOrLoad(date) { expected }

        assertEquals(expected, result)
        verify(lock).unlock()
    }
}
