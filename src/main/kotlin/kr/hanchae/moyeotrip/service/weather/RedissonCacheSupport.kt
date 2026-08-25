package kr.hanchae.moyeotrip.service.weather

import org.redisson.api.RBucket
import org.redisson.api.RedissonClient
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.concurrent.TimeUnit

abstract class RedissonCacheSupport<T>(
    private val redissonClient: RedissonClient,
    private val cacheName: String,
) {
    protected fun getOrLoad(
        keyParts: List<Any>,
        ttl: Duration,
        loader: () -> T?,
    ): T? {
        val cacheKey = cacheKey(keyParts)
        val bucket =
            runCatching { redissonClient.getBucket<String>(cacheKey) }.getOrElse { exception ->
                log.warn("Redis $cacheName 캐시에 접근하지 못했습니다. 원본 데이터를 직접 조회합니다.", exception)
                return loader()
            }
        read(bucket)?.let { return it }

        val lock =
            runCatching { redissonClient.getLock("$cacheKey:lock") }.getOrElse { exception ->
                log.warn("Redis $cacheName 캐시 락을 생성하지 못했습니다. 원본 데이터를 직접 조회합니다.", exception)
                return loader()
            }
        val locked =
            runCatching { lock.tryLock(LOCK_WAIT_SECONDS, LOCK_LEASE_SECONDS, TimeUnit.SECONDS) }
                .getOrElse { exception ->
                    log.warn("Redis $cacheName 캐시 락을 획득하지 못했습니다. 원본 데이터를 직접 조회합니다.", exception)
                    return loader()
                }
        if (!locked) return read(bucket) ?: loader()

        try {
            read(bucket)?.let { return it }
            val value = loader() ?: return null
            runCatching { bucket.set(serialize(value), ttl) }
                .onFailure { exception ->
                    log.warn("Redis $cacheName 캐시 저장에 실패했습니다. 이번 응답에는 조회값을 사용합니다.", exception)
                }
            return value
        } finally {
            if (lock.isHeldByCurrentThread) lock.unlock()
        }
    }

    private fun read(bucket: RBucket<String>): T? =
        runCatching { bucket.get()?.let(::deserialize) }
            .onFailure { exception -> log.warn("Redis $cacheName 캐시 조회에 실패했습니다.", exception) }
            .getOrNull()

    private fun cacheKey(keyParts: List<Any>): String = "$CACHE_KEY_PREFIX$cacheName:${keyParts.joinToString(":")}"

    protected abstract fun serialize(value: T): String

    protected abstract fun deserialize(value: String): T?

    companion object {
        private const val CACHE_KEY_PREFIX = "MoyeoTrip:"
        private const val LOCK_WAIT_SECONDS = 5L
        private const val LOCK_LEASE_SECONDS = 30L
        private val log = LoggerFactory.getLogger(RedissonCacheSupport::class.java)
    }
}
