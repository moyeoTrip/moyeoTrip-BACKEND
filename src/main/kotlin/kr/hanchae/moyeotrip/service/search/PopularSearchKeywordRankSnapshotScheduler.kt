package kr.hanchae.moyeotrip.service.search

import kr.hanchae.moyeotrip.repository.PopularSearchKeywordRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class PopularSearchKeywordRankSnapshotScheduler(
    private val repository: PopularSearchKeywordRepository,
) {
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    fun snapshotDailyRanks() {
        runCatching { repository.snapshotCurrentRanks() }
            .onFailure { exception -> logger.warn("인기 검색어 일일 순위 저장에 실패했습니다.", exception) }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PopularSearchKeywordRankSnapshotScheduler::class.java)
    }
}
