package kr.hanchae.moyeotrip.controller.tour

import kr.hanchae.moyeotrip.controller.tour.response.TourismContentDetailResponse
import kr.hanchae.moyeotrip.controller.tour.response.TourismContentPageResponse
import kr.hanchae.moyeotrip.controller.tour.response.TourismContentTypeResponse
import kr.hanchae.moyeotrip.service.tour.TourismContentService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/tourism-contents")
class TourismContentController(
    private val tourismContentService: TourismContentService,
) : TourismContentAPISpec {
    @GetMapping("/types")
    override fun getContentTypes(): List<TourismContentTypeResponse> = tourismContentService.getContentTypes()

    @GetMapping
    override fun getContents(
        @RequestParam(required = false) contentTypeId: Int?,
        @RequestParam(required = false) keyword: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): TourismContentPageResponse = tourismContentService.getContents(contentTypeId, keyword, page, size.coerceIn(1, 100))

    @GetMapping("/{contentId}")
    override fun getContent(
        @PathVariable contentId: Long,
    ): TourismContentDetailResponse = tourismContentService.getContent(contentId)
}
