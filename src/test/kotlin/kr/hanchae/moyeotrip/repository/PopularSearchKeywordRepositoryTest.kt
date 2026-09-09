package kr.hanchae.moyeotrip.repository

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.redisson.api.RLock
import org.redisson.api.RScoredSortedSet
import org.redisson.api.RedissonClient
import org.redisson.client.codec.StringCodec
import org.redisson.client.protocol.ScoredEntry
import java.util.concurrent.TimeUnit

class PopularSearchKeywordRepositoryTest {
    private val redissonClient = mock(RedissonClient::class.java)

    @Suppress("UNCHECKED_CAST")
    private val keywords = mock(RScoredSortedSet::class.java) as RScoredSortedSet<String>

    @Suppress("UNCHECKED_CAST")
    private val previousRankKeywords = mock(RScoredSortedSet::class.java) as RScoredSortedSet<String>

    @Suppress("UNCHECKED_CAST")
    private val temporarySnapshot = mock(RScoredSortedSet::class.java) as RScoredSortedSet<String>

    private val lock = mock(RLock::class.java)

    @Test
    fun `검색 횟수를 Redis 정렬 집합 점수로 누적한다`() {
        prepareRepository()
        val repository = PopularSearchKeywordRepository(redissonClient)

        repository.increment("주왕산")

        verify(keywords).addScore("주왕산", 1)
    }

    @Test
    fun `점수가 높은 검색어부터 지정한 개수만큼 조회한다`() {
        prepareRepository()
        `when`(keywords.entryRangeReversed(0, 1)).thenReturn(
            listOf(
                ScoredEntry(12.0, "주왕산"),
                ScoredEntry(8.0, "경주 야경"),
            ),
        )
        `when`(previousRankKeywords.revRank(listOf("주왕산", "경주 야경"))).thenReturn(listOf(2, null))
        val repository = PopularSearchKeywordRepository(redissonClient)

        val result = repository.findTop(2)

        assertEquals(
            listOf(PopularSearchKeyword("주왕산", 12, 3), PopularSearchKeyword("경주 야경", 8, null)),
            result,
        )
    }

    @Test
    fun `락을 획득하면 현재 순위를 임시 키에 저장한 뒤 스냅샷 키로 교체한다`() {
        prepareRepository()
        `when`(redissonClient.getLock(PopularSearchKeywordRepository.RANK_SNAPSHOT_LOCK_KEY)).thenReturn(lock)
        `when`(lock.tryLock(0, 5, TimeUnit.MINUTES)).thenReturn(true)
        `when`(lock.isHeldByCurrentThread).thenReturn(true)
        val entries = listOf(ScoredEntry(12.0, "주왕산"), ScoredEntry(8.0, "경주 야경"))
        `when`(keywords.entryRangeReversed(0, -1)).thenReturn(entries)
        val repository = PopularSearchKeywordRepository(redissonClient)

        repository.snapshotCurrentRanks()

        verify(temporarySnapshot).addAll(mapOf("주왕산" to 12.0, "경주 야경" to 8.0))
        verify(temporarySnapshot).rename(PopularSearchKeywordRepository.PREVIOUS_RANK_KEYWORDS_KEY)
        verify(lock).unlock()
    }

    @Test
    fun `스냅샷 락을 획득하지 못하면 순위를 복사하지 않는다`() {
        prepareRepository()
        `when`(redissonClient.getLock(PopularSearchKeywordRepository.RANK_SNAPSHOT_LOCK_KEY)).thenReturn(lock)
        `when`(lock.tryLock(0, 5, TimeUnit.MINUTES)).thenReturn(false)
        val repository = PopularSearchKeywordRepository(redissonClient)

        repository.snapshotCurrentRanks()

        verify(keywords, never()).entryRangeReversed(0, -1)
    }

    @Test
    fun `검색어가 없으면 이전 순위 스냅샷을 비운다`() {
        prepareRepository()
        `when`(redissonClient.getLock(PopularSearchKeywordRepository.RANK_SNAPSHOT_LOCK_KEY)).thenReturn(lock)
        `when`(lock.tryLock(0, 5, TimeUnit.MINUTES)).thenReturn(true)
        `when`(keywords.entryRangeReversed(0, -1)).thenReturn(emptyList())
        val repository = PopularSearchKeywordRepository(redissonClient)

        repository.snapshotCurrentRanks()

        verify(previousRankKeywords).delete()
    }

    private fun prepareRepository() {
        `when`(
            redissonClient.getScoredSortedSet<String>(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.same(StringCodec.INSTANCE),
            ),
        ).thenAnswer { invocation ->
            when (val key = invocation.getArgument<String>(0)) {
                PopularSearchKeywordRepository.POPULAR_SEARCH_KEYWORDS_KEY -> keywords
                PopularSearchKeywordRepository.PREVIOUS_RANK_KEYWORDS_KEY -> previousRankKeywords
                else -> {
                    require(key.startsWith("${PopularSearchKeywordRepository.PREVIOUS_RANK_KEYWORDS_KEY}:temp:"))
                    temporarySnapshot
                }
            }
        }
    }
}
