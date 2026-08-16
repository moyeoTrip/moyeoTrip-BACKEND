package kr.hanchae.moyeotrip.controller.tour.request

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min

data class RateTravelCourseRequest(
    @field:Min(1)
    @field:Max(5)
    val score: Int,
)
