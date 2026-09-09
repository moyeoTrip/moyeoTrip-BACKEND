package kr.hanchae.moyeotrip.repository

import org.redisson.api.RedissonClient
import org.redisson.client.codec.StringCodec
import org.springframework.stereotype.Repository
import java.util.UUID
import java.util.concurrent.TimeUnit

@Repository
class PopularSearchKeywordRepository(
    private val redissonClient: RedissonClient,
) {
    private val keywords = scoredSortedSet(POPULAR_SEARCH_KEYWORDS_KEY)
    private val previousRankKeywords = scoredSortedSet(PREVIOUS_RANK_KEYWORDS_KEY)

    fun increment(keyword: String) {
        keywords.addScore(keyword, 1)
    }

    fun findTop(limit: Int): List<PopularSearchKeyword> {
        val entries = keywords.entryRangeReversed(0, limit - 1).toList()
        if (entries.isEmpty()) return emptyList()
        val previousRanks = previousRankKeywords.revRank(entries.map { it.value })
        return entries.mapIndexed { index, entry ->
            PopularSearchKeyword(
                keyword = entry.value,
                searchCount = entry.score.toLong(),
                previousRank = previousRanks[index]?.plus(1),
            )
        }
    }

    fun snapshotCurrentRanks() {
        val lock = redissonClient.getLock(RANK_SNAPSHOT_LOCK_KEY)
        if (!lock.tryLock(0, RANK_SNAPSHOT_LOCK_MINUTES, TimeUnit.MINUTES)) return
        try {
            val currentEntries = keywords.entryRangeReversed(0, -1)
            if (currentEntries.isEmpty()) {
                previousRankKeywords.delete()
                return
            }

            val temporarySnapshotKey = "$PREVIOUS_RANK_KEYWORDS_KEY:temp:${UUID.randomUUID()}"
            val temporarySnapshot = scoredSortedSet(temporarySnapshotKey)
            try {
                temporarySnapshot.addAll(currentEntries.associate { it.value to it.score })
                temporarySnapshot.rename(PREVIOUS_RANK_KEYWORDS_KEY)
            } catch (exception: Exception) {
                temporarySnapshot.delete()
                throw exception
            }
        } finally {
            if (lock.isHeldByCurrentThread) lock.unlock()
        }
    }

    private fun scoredSortedSet(key: String) =
        redissonClient.getScoredSortedSet<String>(
            key,
            StringCodec.INSTANCE,
        )

    companion object {
        const val POPULAR_SEARCH_KEYWORDS_KEY = "MoyeoTrip:popular-search-keywords"
        const val PREVIOUS_RANK_KEYWORDS_KEY = "MoyeoTrip:popular-search-keywords:previous-rank"
        const val RANK_SNAPSHOT_LOCK_KEY = "MoyeoTrip:popular-search-keywords:rank-snapshot:lock"
        private const val RANK_SNAPSHOT_LOCK_MINUTES = 5L
    }
}

data class PopularSearchKeyword(
    val keyword: String,
    val searchCount: Long,
    val previousRank: Int? = null,
)
