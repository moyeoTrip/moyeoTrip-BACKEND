package kr.hanchae.moyeotrip.service.search

import kr.hanchae.moyeotrip.controller.search.response.PopularSearchKeywordResponse
import kr.hanchae.moyeotrip.repository.PopularSearchKeywordRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class PopularSearchKeywordService(
    private val repository: PopularSearchKeywordRepository,
) {
    fun record(keyword: String?) {
        val normalizedKeyword = keyword.normalize() ?: return
        runCatching { repository.increment(normalizedKeyword) }
            .onFailure { exception -> logger.warn("인기 검색어 집계를 건너뜁니다. keyword={}", normalizedKeyword, exception) }
    }

    fun getPopularKeywords(limit: Int): List<PopularSearchKeywordResponse> =
        runCatching {
            repository.findTop(limit).mapIndexed { index, keyword ->
                PopularSearchKeywordResponse(
                    rank = index + 1,
                    keyword = keyword.keyword,
                    searchCount = keyword.searchCount,
                )
            }
        }.onFailure { exception ->
            logger.warn("인기 검색어 조회에 실패해 빈 목록을 반환합니다.", exception)
        }.getOrDefault(emptyList())

    private fun String?.normalize(): String? =
        this
            ?.trim()
            ?.replace(WHITESPACE_REGEX, " ")
            ?.lowercase()
            ?.takeIf(String::isNotEmpty)

    companion object {
        private val WHITESPACE_REGEX = Regex("\\s+")
        private val logger = LoggerFactory.getLogger(PopularSearchKeywordService::class.java)
    }
}
