package kr.hanchae.moyeotrip.controller.tour.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class PublishTravelCourseRequest(
    @field:NotBlank
    @field:Size(max = 100)
    val title: String,
    @field:NotBlank
    @field:Size(max = 500)
    val description: String,
    val showCreatorNickname: Boolean = true,
)
