package kr.hanchae.moyeotrip.controller.search

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import kr.hanchae.moyeotrip.controller.search.response.PopularSearchKeywordResponse

@Tag(name = "검색", description = "통합 검색 부가 API")
interface SearchAPISpec {
    @Operation(
        summary = "인기 검색어 조회",
        description = "모임 통합 검색에서 사용자가 입력한 검색어를 누적 집계해 검색 횟수가 많은 순서로 반환합니다.",
    )
    fun getPopularKeywords(
        @Parameter(description = "반환할 검색어 개수. 1~20 범위로 보정됩니다.", example = "10") limit: Int,
    ): List<PopularSearchKeywordResponse>
}
