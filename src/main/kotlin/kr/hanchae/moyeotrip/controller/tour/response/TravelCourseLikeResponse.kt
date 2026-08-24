package kr.hanchae.moyeotrip.controller.tour.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "공개 여행 코스 좋아요 변경 결과")
data class TravelCourseLikeResponse(
    @field:Schema(description = "변경 후 로그인 사용자의 좋아요 여부", example = "true")
    val liked: Boolean,
    @field:Schema(description = "변경 후 코스 좋아요 수", example = "12")
    val likeCount: Long,
)
