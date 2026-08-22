package kr.hanchae.moyeotrip.controller.user

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import kr.hanchae.moyeotrip.controller.user.request.ReviewTravelCompanionRequest
import kr.hanchae.moyeotrip.controller.user.response.TravelDexResponse
import kr.hanchae.moyeotrip.controller.user.response.TripCompanionResponse

@Tag(name = "여행 동행자", description = "함께 여행한 사용자 평가와 여행 도감 API")
@SecurityRequirement(name = "Authorization")
interface TravelCompanionAPISpec {
    @Operation(summary = "여행 동행자 목록", description = "완료한 채팅방의 동행자와 로그인 사용자가 남긴 평가 정보를 반환합니다.")
    fun getTripCompanions(
        @Parameter(hidden = true) userId: Long,
        roomId: Long,
    ): List<TripCompanionResponse>

    @Operation(summary = "여행 동행자 평가", description = "함께 완료한 여행의 동행자에게 매너 점수와 한줄평을 등록 또는 수정합니다.")
    fun reviewCompanion(
        @Parameter(hidden = true) userId: Long,
        roomId: Long,
        companionId: Long,
        request: ReviewTravelCompanionRequest,
    ): TripCompanionResponse

    @Operation(summary = "내 여행 도감 조회", description = "함께 여행한 동행자와 여행 기록을 도감 형태로 반환합니다.")
    fun getMyTravelDex(
        @Parameter(hidden = true) userId: Long,
    ): TravelDexResponse
}
