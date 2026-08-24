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
import kr.hanchae.moyeotrip.controller.user.request.ReviewTravelCompanionRequest
import kr.hanchae.moyeotrip.controller.user.response.TravelDexResponse
import kr.hanchae.moyeotrip.controller.user.response.TripCompanionResponse
import kr.hanchae.moyeotrip.exception.ErrorResponse

@Tag(name = "여행 동행자", description = "함께 여행한 사용자 평가와 여행 도감 API")
interface TravelCompanionAPISpec {
    @Operation(summary = "여행 동행자 목록", description = "완료한 채팅방의 동행자와 로그인 사용자가 남긴 평가 정보를 반환합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "여행 동행자 목록 조회 성공",
                content = [Content(array = ArraySchema(schema = Schema(implementation = TripCompanionResponse::class)))],
            ),
            ApiResponse(
                responseCode = "403",
                description = "채팅방 참가자가 아니어서 동행자를 조회할 수 없음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = TravelCompanionSwaggerExamples.NOT_PARTICIPANT)],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "404",
                description = "채팅방을 찾을 수 없음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = TravelCompanionSwaggerExamples.CHAT_ROOM_NOT_FOUND)],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "409",
                description = "여행이 아직 완료되지 않음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = TravelCompanionSwaggerExamples.TRIP_NOT_COMPLETED)],
                    ),
                ],
            ),
        ],
    )
    fun getTripCompanions(
        @Parameter(hidden = true) userId: Long,
        @Parameter(description = "완료한 여행 동행자를 조회할 채팅방 ID", example = "101")
        roomId: Long,
    ): List<TripCompanionResponse>

    @Operation(summary = "여행 동행자 평가", description = "함께 완료한 여행의 동행자에게 매너 점수와 한줄평을 등록 또는 수정합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "여행 동행자 평가 등록 또는 수정 성공",
                content = [Content(schema = Schema(implementation = TripCompanionResponse::class))],
            ),
            ApiResponse(
                responseCode = "400",
                description = "매너 점수 또는 한줄평 입력값이 유효하지 않음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = TravelCompanionSwaggerExamples.BAD_REQUEST)],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "403",
                description = "자기 자신을 평가하거나 완료한 여행의 참가자가 아님",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = TravelCompanionSwaggerExamples.FORBIDDEN)],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "404",
                description = "채팅방·동행자 또는 동행 기록을 찾을 수 없음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [
                            ExampleObject(name = "채팅방 없음", value = TravelCompanionSwaggerExamples.CHAT_ROOM_NOT_FOUND),
                            ExampleObject(name = "함께 여행한 동행 기록 없음", value = TravelCompanionSwaggerExamples.TRAVEL_COMPANION_NOT_FOUND),
                            ExampleObject(name = "평가할 동행자 없음", value = TravelCompanionSwaggerExamples.USER_NOT_FOUND),
                        ],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "409",
                description = "여행이 아직 완료되지 않음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = TravelCompanionSwaggerExamples.TRIP_NOT_COMPLETED)],
                    ),
                ],
            ),
        ],
    )
    fun reviewCompanion(
        @Parameter(hidden = true) userId: Long,
        @Parameter(description = "함께 여행한 채팅방 ID", example = "101")
        roomId: Long,
        @Parameter(description = "평가할 함께 여행한 상대 사용자 ID", example = "202")
        companionId: Long,
        @Parameter(description = "매너 점수와 한줄평", required = true)
        request: ReviewTravelCompanionRequest,
    ): TripCompanionResponse

    @Operation(summary = "내 여행 도감 조회", description = "함께 여행한 동행자와 여행 기록을 도감 형태로 반환합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "내 여행 도감 조회 성공",
                content = [Content(schema = Schema(implementation = TravelDexResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "로그인 사용자를 찾을 수 없음",
                content = [
                    Content(
                        schema = Schema(implementation = ErrorResponse::class),
                        examples = [ExampleObject(value = TravelCompanionSwaggerExamples.USER_NOT_FOUND)],
                    ),
                ],
            ),
        ],
    )
    fun getMyTravelDex(
        @Parameter(hidden = true) userId: Long,
    ): TravelDexResponse
}

private object TravelCompanionSwaggerExamples {
    const val BAD_REQUEST = """{"code":40000,"errorMessage":"잘못된 요청입니다."}"""
    const val FORBIDDEN = """{"code":40300,"errorMessage":"접근 권한이 없습니다."}"""
    const val NOT_PARTICIPANT = """{"code":40301,"errorMessage":"사용자가 채팅방에 참여하고 있지 않습니다."}"""
    const val TRIP_NOT_COMPLETED = """{"code":40915,"errorMessage":"아직 완료되지 않은 여행입니다."}"""
    const val CHAT_ROOM_NOT_FOUND = """{"code":40405,"errorMessage":"채팅방을 찾을 수 없습니다."}"""
    const val TRAVEL_COMPANION_NOT_FOUND = """{"code":40402,"errorMessage":"요청한 리소스를 찾을 수 없습니다."}"""
    const val USER_NOT_FOUND = """{"code":40400,"errorMessage":"해당 유저를 찾을 수 없습니다."}"""
}
