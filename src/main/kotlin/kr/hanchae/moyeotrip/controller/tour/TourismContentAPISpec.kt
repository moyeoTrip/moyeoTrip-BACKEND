package kr.hanchae.moyeotrip.controller.tour

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import kr.hanchae.moyeotrip.controller.tour.response.TourismContentDetailResponse
import kr.hanchae.moyeotrip.controller.tour.response.TourismContentPageResponse
import kr.hanchae.moyeotrip.controller.tour.response.TourismContentTypeResponse

@Tag(name = "여행지", description = "한국관광공사 여행지 목록 및 상세 API")
@SecurityRequirement(name = "Authorization")
interface TourismContentAPISpec {
    @Operation(summary = "관광 콘텐츠 타입 목록 조회", description = "여행지 목록을 유형별로 필터링할 때 사용하는 관광 콘텐츠 타입을 반환합니다.")
    fun getContentTypes(): List<TourismContentTypeResponse>

    @Operation(summary = "여행지 목록 조회", description = "contentTypeId를 생략하면 전체 여행지를 페이지 단위로 조회합니다. size는 1~100 범위로 적용됩니다.")
    fun getContents(
        contentTypeId: Int?,
        page: Int,
        size: Int,
    ): TourismContentPageResponse

    @Operation(summary = "여행지 상세 조회", description = "최초 조회 시 한국관광공사 상세 API를 호출해 DB를 채웁니다.")
    fun getContent(contentId: Long): TourismContentDetailResponse
}
