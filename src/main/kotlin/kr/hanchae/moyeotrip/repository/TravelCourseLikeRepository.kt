package kr.hanchae.moyeotrip.repository

import kr.hanchae.moyeotrip.entity.tour.TravelCourseLike
import org.springframework.data.jpa.repository.JpaRepository

interface TravelCourseLikeRepository : JpaRepository<TravelCourseLike, Long> {
    fun findByCourseIdAndUserId(
        courseId: Long,
        userId: Long,
    ): TravelCourseLike?

    fun countByCourseId(courseId: Long): Long
}
