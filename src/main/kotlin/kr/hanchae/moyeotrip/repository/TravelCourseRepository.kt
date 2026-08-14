package kr.hanchae.moyeotrip.repository

import kr.hanchae.moyeotrip.entity.tour.TravelCourse
import kr.hanchae.moyeotrip.entity.tour.TravelCourseType
import org.springframework.data.jpa.repository.JpaRepository

interface TravelCourseRepository : JpaRepository<TravelCourse, Long> {
    fun findByIdAndType(
        id: Long,
        type: TravelCourseType,
    ): TravelCourse?

    fun findAllByTypeOrderByCreatedDateTimeDesc(type: TravelCourseType): List<TravelCourse>
}
