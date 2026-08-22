package kr.hanchae.moyeotrip.controller.user.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Size

@Schema(description = "함께 여행한 동행자 평가 요청")
data class ReviewTravelCompanionRequest(
    @field:Schema(description = "동행자 매너 점수", example = "5", minimum = "1", maximum = "5")
    @field:Min(1)
    @field:Max(5)
    val mannerScore: Int,
    @field:Schema(description = "동행자에게 남길 한줄평", example = "약속 시간을 잘 지키는 좋은 동행자예요.", nullable = true)
    @field:Size(max = 40)
    val oneLineReview: String? = null,
)
