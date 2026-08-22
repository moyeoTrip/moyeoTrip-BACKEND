package kr.hanchae.moyeotrip.controller.user.request

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Size

data class ReviewTravelCompanionRequest(
    @field:Min(1) @field:Max(5)
    val mannerScore: Int,
    @field:Size(max = 40)
    val oneLineReview: String? = null,
)
