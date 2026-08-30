package kr.hanchae.moyeotrip.repository

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.redisson.api.RScoredSortedSet
import org.redisson.api.RedissonClient
import org.redisson.client.codec.StringCodec
import org.redisson.client.protocol.ScoredEntry

class PopularSearchKeywordRepositoryTest {
    private val redissonClient = mock(RedissonClient::class.java)

    @Suppress("UNCHECKED_CAST")
    private val keywords = mock(RScoredSortedSet::class.java) as RScoredSortedSet<String>

    @Test
    fun `검색 횟수를 Redis 정렬 집합 점수로 누적한다`() {
        `when`(
            redissonClient.getScoredSortedSet<String>(
                PopularSearchKeywordRepository.POPULAR_SEARCH_KEYWORDS_KEY,
                StringCodec.INSTANCE,
            ),
        ).thenReturn(keywords)
        val repository = PopularSearchKeywordRepository(redissonClient)

        repository.increment("주왕산")

        verify(keywords).addScore("주왕산", 1)
    }

    @Test
    fun `점수가 높은 검색어부터 지정한 개수만큼 조회한다`() {
        `when`(
            redissonClient.getScoredSortedSet<String>(
                PopularSearchKeywordRepository.POPULAR_SEARCH_KEYWORDS_KEY,
                StringCodec.INSTANCE,
            ),
        ).thenReturn(keywords)
        `when`(keywords.entryRangeReversed(0, 1)).thenReturn(
            listOf(
                ScoredEntry(12.0, "주왕산"),
                ScoredEntry(8.0, "경주 야경"),
            ),
        )
        val repository = PopularSearchKeywordRepository(redissonClient)

        val result = repository.findTop(2)

        assertEquals(
            listOf(PopularSearchKeyword("주왕산", 12), PopularSearchKeyword("경주 야경", 8)),
            result,
        )
    }
}
