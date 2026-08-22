package kr.hanchae.moyeotrip.controller.tour.response

import kr.hanchae.moyeotrip.entity.tour.CoursePublicationStatus

data class CoursePublicationResponse(
    val courseId: Long,
    val publicationStatus: CoursePublicationStatus,
)
