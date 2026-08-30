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
)
