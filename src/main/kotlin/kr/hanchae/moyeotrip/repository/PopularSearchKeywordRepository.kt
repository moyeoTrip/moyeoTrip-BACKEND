package kr.hanchae.moyeotrip.repository

import org.redisson.api.RedissonClient
import org.redisson.client.codec.StringCodec
import org.springframework.stereotype.Repository

@Repository
class PopularSearchKeywordRepository(
    redissonClient: RedissonClient,
) {
    private val keywords = redissonClient.getScoredSortedSet<String>(POPULAR_SEARCH_KEYWORDS_KEY, StringCodec.INSTANCE)

    fun increment(keyword: String) {
        keywords.addScore(keyword, 1)
    }

    fun findTop(limit: Int): List<PopularSearchKeyword> =
        keywords
            .entryRangeReversed(0, limit - 1)
            .map { PopularSearchKeyword(keyword = it.value, searchCount = it.score.toLong()) }

    companion object {
        const val POPULAR_SEARCH_KEYWORDS_KEY = "MoyeoTrip:popular-search-keywords"
    }
}

data class PopularSearchKeyword(
    val keyword: String,
    val searchCount: Long,
)
