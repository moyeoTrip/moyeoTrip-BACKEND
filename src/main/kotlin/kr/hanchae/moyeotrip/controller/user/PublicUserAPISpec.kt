package kr.hanchae.moyeotrip.controller.user

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import kr.hanchae.moyeotrip.config.swagger.SwaggerTag
import kr.hanchae.moyeotrip.controller.user.response.PublicProfileResponse
import kr.hanchae.moyeotrip.controller.user.response.ReceivedTravelReviewResponse
import kr.hanchae.moyeotrip.exception.ErrorResponse

@Tag(name = SwaggerTag.USER, description = "다른 사용자의 공개 프로필 조회 API")
interface PublicUserAPISpec {
    @Operation(
        summary = "다른 사용자 프로필 조회",
        description = "닉네임, 닉네임 표시 색상, 프로필 이미지, 자기소개, 여행 스타일, 관심 지역과 매너 점수만 반환합니다. 생년월일·성별·알림 설정은 반환하지 않습니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "다른 사용자 프로필 조회 성공",
                content = [Content(schema = Schema(implementation = PublicProfileResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "사용자를 찾을 수 없거나 탈퇴한 사용자임",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = """{"code":40400,"errorMessage":"해당 유저를 찾을 수 없습니다."}""")],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "401",
                description = "서비스 Access Token이 없거나 유효하지 않음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = """{"code":40100,"errorMessage":"인증되지 않은 사용자입니다."}""")],
                    ),
                ],
            ),
        ],
    )
    fun getPublicProfile(
        @Parameter(description = "조회할 사용자 ID", example = "12") userId: Long,
    ): PublicProfileResponse

    @Operation(summary = "사용자에게 받은 여행 한줄평 목록", description = "대상 사용자에게 완료 여행 동행자가 남긴 비어 있지 않은 한줄평을 최신순으로 반환합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "여행 한줄평 목록 조회 성공",
                content = [Content(array = ArraySchema(schema = Schema(implementation = ReceivedTravelReviewResponse::class)))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "사용자를 찾을 수 없거나 탈퇴한 사용자임",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = """{"code":40400,"errorMessage":"해당 유저를 찾을 수 없습니다."}""")],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "401",
                description = "서비스 Access Token이 없거나 유효하지 않음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = """{"code":40100,"errorMessage":"인증되지 않은 사용자입니다."}""")],
                    ),
                ],
            ),
        ],
    )
    fun getReceivedTravelReviews(
        @Parameter(description = "한줄평을 조회할 사용자 ID", example = "12") userId: Long,
    ): List<ReceivedTravelReviewResponse>
}
