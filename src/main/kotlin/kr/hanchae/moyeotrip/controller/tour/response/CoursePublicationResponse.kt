package kr.hanchae.moyeotrip.controller.tour.response

import io.swagger.v3.oas.annotations.media.Schema
import kr.hanchae.moyeotrip.entity.tour.CoursePublicationStatus

@Schema(description = "커스텀 여행 코스 공개 결과")
data class CoursePublicationResponse(
    @field:Schema(description = "공개 처리한 여행 코스 ID", example = "77")
    val courseId: Long,
    @field:Schema(description = "여행 코스 공개 상태", example = "PUBLISHED")
    val publicationStatus: CoursePublicationStatus,
)
