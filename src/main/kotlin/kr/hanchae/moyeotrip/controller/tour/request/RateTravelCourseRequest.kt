package kr.hanchae.moyeotrip.controller.tour.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min

@Schema(description = "완료한 여행 코스 평점 등록 요청")
data class RateTravelCourseRequest(
    @field:Schema(description = "여행 코스 평점", example = "5", minimum = "1", maximum = "5")
    @field:Min(1)
    @field:Max(5)
    val score: Int,
)
