package kr.hanchae.moyeotrip.controller.tour

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import kr.hanchae.moyeotrip.controller.tour.response.TourismContentDetailResponse
import kr.hanchae.moyeotrip.controller.tour.response.TourismContentPageResponse
import kr.hanchae.moyeotrip.controller.tour.response.TourismContentTypeResponse
import kr.hanchae.moyeotrip.exception.ErrorResponse

@Tag(name = "여행지", description = "한국관광공사 여행지 목록 및 상세 API")
interface TourismContentAPISpec {
    @Operation(summary = "관광 콘텐츠 타입 목록 조회", description = "여행지 목록을 유형별로 필터링할 때 사용하는 관광 콘텐츠 타입을 반환합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "관광 콘텐츠 타입 목록 조회 성공",
                content = [Content(schema = Schema(implementation = TourismContentTypeResponse::class))],
            ),
            ApiResponse(
                responseCode = "500",
                description = "여행지 데이터 조회 실패",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = TourismContentSwaggerExamples.INTERNAL_SERVER_ERROR)],
                    ),
                ],
            ),
        ],
    )
    fun getContentTypes(): List<TourismContentTypeResponse>

    @Operation(summary = "여행지 목록 조회", description = "contentTypeId를 생략하면 전체 여행지를 페이지 단위로 조회합니다. size는 1~100 범위로 적용됩니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "여행지 목록 조회 성공",
                content = [Content(schema = Schema(implementation = TourismContentPageResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "유효하지 않은 콘텐츠 타입 또는 조회 조건",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = TourismContentSwaggerExamples.TOURISM_CONTENT_TYPE_NOT_FOUND)],
                    ),
                ],
            ),
        ],
    )
    fun getContents(
        contentTypeId: Int?,
        page: Int,
        size: Int,
    ): TourismContentPageResponse

    @Operation(summary = "여행지 상세 조회", description = "최초 조회 시 한국관광공사 상세 API를 호출해 DB를 채웁니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "여행지 상세 조회 성공",
                content = [Content(schema = Schema(implementation = TourismContentDetailResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "여행지를 찾을 수 없음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = TourismContentSwaggerExamples.TOURISM_CONTENT_NOT_FOUND)],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "500",
                description = "한국관광공사 상세 정보 조회 실패",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = TourismContentSwaggerExamples.INTERNAL_SERVER_ERROR)],
                    ),
                ],
            ),
        ],
    )
    fun getContent(contentId: Long): TourismContentDetailResponse
}

private object TourismContentSwaggerExamples {
    const val TOURISM_CONTENT_TYPE_NOT_FOUND = """{"code":40409,"errorMessage":"관광 콘텐츠 타입을 찾을 수 없습니다."}"""
    const val TOURISM_CONTENT_NOT_FOUND = """{"code":40408,"errorMessage":"관광 콘텐츠를 찾을 수 없습니다."}"""
    const val INTERNAL_SERVER_ERROR = """{"code":50000,"errorMessage":"서버에러입니다."}"""
}
