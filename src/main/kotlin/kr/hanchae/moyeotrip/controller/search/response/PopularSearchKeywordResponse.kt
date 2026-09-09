package kr.hanchae.moyeotrip.controller.search.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "인기 검색어")
data class PopularSearchKeywordResponse(
    @field:Schema(description = "인기 순위", example = "1")
    val rank: Int,
    @field:Schema(description = "검색어", example = "주왕산")
    val keyword: String,
    @field:Schema(description = "누적 검색 횟수", example = "128")
    val searchCount: Long,
    @field:Schema(description = "전일 순위 대비 등락 상태", example = "UP")
    val rankTrend: PopularSearchRankTrend,
    @field:Schema(
        description = "전일 순위 대비 변동 폭. 양수는 상승, 음수는 하락이며 신규 진입은 null",
        example = "2",
        nullable = true,
    )
    val rankChange: Int?,
)

enum class PopularSearchRankTrend {
    UP,
    DOWN,
    SAME,
    NEW,
}
