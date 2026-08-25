package kr.hanchae.moyeotrip.service.weather

import kr.hanchae.moyeotrip.client.AirQuality
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
}
