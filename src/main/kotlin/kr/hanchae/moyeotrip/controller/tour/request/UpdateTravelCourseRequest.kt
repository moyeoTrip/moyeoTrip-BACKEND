package kr.hanchae.moyeotrip.controller.tour.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.Size
import kr.hanchae.moyeotrip.controller.chat.request.CustomCoursePlaceRequest

@Schema(description = "채팅방 커스텀 여행 코스 수정 요청")
data class UpdateTravelCourseRequest(
    @field:Schema(description = "방문 순서와 시간을 포함한 변경할 장소 목록. 최소 2개가 필요합니다.")
    @field:Valid
    @field:Size(min = 2)
    val places: List<CustomCoursePlaceRequest>,
)
