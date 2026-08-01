package kr.hanchae.moyeotrip.repository

import kr.hanchae.moyeotrip.entity.user.NicknameColor
import org.redisson.api.RedissonClient
import org.springframework.stereotype.Repository
import java.time.Duration

@Repository
class NicknameCandidateRepository(
    private val redissonClient: RedissonClient,
) {
    fun save(
        selectionToken: String,
        candidates: Map<String, NicknameColor>,
        ttl: Duration,
    ) {
        redissonClient
            .getBucket<String>(cacheKey(selectionToken))
            .set(
                candidates.entries.joinToString(CANDIDATE_SEPARATOR) { (nickname, color) ->
                    "$nickname$VALUE_SEPARATOR${color.name}"
                },
                ttl,
            )
    }

    fun consume(selectionToken: String): Map<String, NicknameColor>? =
        redissonClient
            .getBucket<String>(cacheKey(selectionToken))
            .getAndDelete()
            ?.split(CANDIDATE_SEPARATOR)
            ?.associate { candidate ->
                val (nickname, color) = candidate.split(VALUE_SEPARATOR, limit = 2)
                nickname to NicknameColor.valueOf(color)
            }

    private fun cacheKey(selectionToken: String): String = "$CACHE_PREFIX$selectionToken"

    companion object {
        private const val CACHE_PREFIX = "MoyeoTrip:nickname-candidates:"
        private const val CANDIDATE_SEPARATOR = "\u001F"
        private const val VALUE_SEPARATOR = "\u001E"
    }
}
