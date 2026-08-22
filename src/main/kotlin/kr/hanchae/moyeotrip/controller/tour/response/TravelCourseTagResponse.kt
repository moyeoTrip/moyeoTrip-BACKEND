package kr.hanchae.moyeotrip.controller.tour.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "여행 코스 태그 정보")
data class TravelCourseTagResponse(
    @field:Schema(description = "여행 코스 태그 ID", example = "1")
    val tagId: Long,
    @field:Schema(description = "여행 코스 태그명", example = "자연")
    val name: String,
)
