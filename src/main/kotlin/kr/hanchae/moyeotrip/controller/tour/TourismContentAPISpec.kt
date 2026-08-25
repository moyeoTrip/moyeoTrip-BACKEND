package kr.hanchae.moyeotrip.controller.tour

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
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
import org.springframework.http.ResponseEntity

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

    @Operation(summary = "여행지 목록 조회", description = "유형과 검색어로 여행지를 검색합니다. contentTypeId와 keyword를 모두 생략하면 전체 여행지를 조회합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "여행지 목록 조회 성공",
                content = [Content(schema = Schema(implementation = TourismContentPageResponse::class))],
            ),
            ApiResponse(
                responseCode = "400",
                description = "코스 관광 콘텐츠는 여행지 목록에서 조회할 수 없음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = TourismContentSwaggerExamples.TOURISM_COURSE_CONTENT_NOT_LISTED)],
                    ),
                ],
            ),
        ],
    )
    fun getContents(
        @Parameter(description = "관광 콘텐츠 유형 ID. 생략하면 모든 일반 여행지를 조회합니다. 코스 유형은 허용하지 않습니다.", example = "12")
        contentTypeId: Int?,
        @Parameter(description = "여행지 제목 또는 주소에 포함되는 검색어. 앞뒤 공백을 제거하며 빈 문자열은 전체 조회로 처리합니다.", example = "주왕산")
        keyword: String?,
        @Parameter(description = "1부터 시작하는 페이지 번호. 기본값은 1입니다.", example = "1")
        page: Int,
        @Parameter(description = "페이지당 항목 수. 1~100으로 보정되며 기본값은 20입니다.", example = "20")
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
    fun getContent(
        @Parameter(description = "조회할 TourismContent ID", example = "126508")
        contentId: Long,
    ): TourismContentDetailResponse

    @Operation(
        summary = "관광 이미지 프록시 조회",
        description = "브라우저 CORS 제한을 피하기 위해 공식 VisitKorea HTTPS 이미지 주소만 서버가 대신 조회해 반환합니다. 최대 20MB 이미지 파일만 허용합니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "관광 이미지 조회 성공", content = [Content(mediaType = "image/jpeg")]),
            ApiResponse(
                responseCode = "400",
                description = "허용되지 않은 이미지 URL",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = TourismContentSwaggerExamples.INVALID_TOURISM_IMAGE_URL)],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "502",
                description = "관광 이미지 원본 조회 실패",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = TourismContentSwaggerExamples.TOURISM_IMAGE_FETCH_FAILED)],
                    ),
                ],
            ),
        ],
    )
    fun getImage(
        @Parameter(
            description = "TourismContent 응답의 thumbnail 또는 originalImageUrl. visitkorea.or.kr 하위 HTTPS 주소만 허용합니다.",
            example = "https://tong.visitkorea.or.kr/cms/resource/00/000000_image.jpg",
        )
        imageUrl: String,
    ): ResponseEntity<ByteArray>
}

private object TourismContentSwaggerExamples {
    const val TOURISM_COURSE_CONTENT_NOT_LISTED = """{"code":40005,"errorMessage":"코스 관광 콘텐츠는 여행지 목록에서 조회할 수 없습니다."}"""
    const val TOURISM_CONTENT_NOT_FOUND = """{"code":40408,"errorMessage":"관광 콘텐츠를 찾을 수 없습니다."}"""
    const val INVALID_TOURISM_IMAGE_URL = """{"code":40037,"errorMessage":"관광 이미지 URL은 공식 VisitKorea HTTPS 이미지 주소여야 합니다."}"""
    const val TOURISM_IMAGE_FETCH_FAILED = """{"code":50204,"errorMessage":"관광 이미지 원본을 현재 불러올 수 없습니다."}"""
    const val INTERNAL_SERVER_ERROR = """{"code":50000,"errorMessage":"서버에러입니다."}"""
}
