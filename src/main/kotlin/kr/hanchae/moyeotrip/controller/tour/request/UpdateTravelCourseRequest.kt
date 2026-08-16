package kr.hanchae.moyeotrip.controller.tour.request

import jakarta.validation.Valid
import jakarta.validation.constraints.Size
import kr.hanchae.moyeotrip.controller.chat.request.CustomCoursePlaceRequest

data class UpdateTravelCourseRequest(
    @field:Valid
    @field:Size(min = 2)
    val places: List<CustomCoursePlaceRequest>,
)
