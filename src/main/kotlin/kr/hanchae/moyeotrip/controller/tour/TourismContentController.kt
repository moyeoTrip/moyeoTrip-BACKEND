package kr.hanchae.moyeotrip.controller.tour

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import kr.hanchae.moyeotrip.controller.tour.response.TourismContentDetailResponse
import kr.hanchae.moyeotrip.controller.tour.response.TourismContentPageResponse
import kr.hanchae.moyeotrip.controller.tour.response.TourismContentTypeResponse
import kr.hanchae.moyeotrip.service.tour.TourismContentService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "여행지", description = "한국관광공사 여행지 목록 및 상세 API")
@SecurityRequirement(name = "Authorization")
@RestController
@RequestMapping("/api/v1/tourism-contents")
class TourismContentController(
    private val tourismContentService: TourismContentService,
) {
    @Operation(summary = "관광 콘텐츠 타입 목록 조회")
    @GetMapping("/types")
    fun getContentTypes(): List<TourismContentTypeResponse> = tourismContentService.getContentTypes()

    @Operation(summary = "여행지 목록 조회")
    @GetMapping
    fun getContents(
        @RequestParam(required = false) contentTypeId: Int?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): TourismContentPageResponse = tourismContentService.getContents(contentTypeId, page, size.coerceIn(1, 100))

    @Operation(summary = "여행지 상세 조회", description = "최초 조회 시 한국관광공사 상세 API를 호출해 DB를 채웁니다.")
    @GetMapping("/{contentId}")
    fun getContent(
        @PathVariable contentId: Long,
    ): TourismContentDetailResponse = tourismContentService.getContent(contentId)
}
