package kr.hanchae.moyeotrip.service.search

import kr.hanchae.moyeotrip.repository.PopularSearchKeywordRepository
import org.junit.jupiter.api.Test
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

class PopularSearchKeywordRankSnapshotSchedulerTest {
    private val repository = mock(PopularSearchKeywordRepository::class.java)
    private val scheduler = PopularSearchKeywordRankSnapshotScheduler(repository)

    @Test
    fun `매일 현재 인기 검색어 순위를 저장한다`() {
        scheduler.snapshotDailyRanks()

        verify(repository).snapshotCurrentRanks()
    }

    @Test
    fun `Redis 오류가 나도 스케줄러는 예외를 전파하지 않는다`() {
        doThrow(IllegalStateException("redis down")).`when`(repository).snapshotCurrentRanks()

        scheduler.snapshotDailyRanks()

        verify(repository).snapshotCurrentRanks()
    }
}
