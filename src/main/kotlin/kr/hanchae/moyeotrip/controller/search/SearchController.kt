package kr.hanchae.moyeotrip.controller.search

import kr.hanchae.moyeotrip.controller.search.response.PopularSearchKeywordResponse
import kr.hanchae.moyeotrip.service.search.PopularSearchKeywordService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/search")
class SearchController(
    private val popularSearchKeywordService: PopularSearchKeywordService,
) : SearchAPISpec {
    @GetMapping("/popular-keywords")
    override fun getPopularKeywords(
        @RequestParam(defaultValue = "10") limit: Int,
    ): List<PopularSearchKeywordResponse> = popularSearchKeywordService.getPopularKeywords(limit.coerceIn(1, MAX_POPULAR_KEYWORD_LIMIT))

    companion object {
        private const val MAX_POPULAR_KEYWORD_LIMIT = 20
    }
}
