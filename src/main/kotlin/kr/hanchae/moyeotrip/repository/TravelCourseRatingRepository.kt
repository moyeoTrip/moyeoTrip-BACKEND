package kr.hanchae.moyeotrip.repository

import kr.hanchae.moyeotrip.entity.tour.TravelCourseRating
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface TravelCourseRatingRepository : JpaRepository<TravelCourseRating, Long> {
    fun findByChatRoomIdAndUserId(
        chatRoomId: Long,
        userId: Long,
    ): TravelCourseRating?

    @Query("SELECT AVG(rating.score) FROM TravelCourseRating rating WHERE rating.course.id = :courseId")
    fun findAverageByCourseId(
        @Param("courseId") courseId: Long,
    ): Double?

    fun countByCourseId(courseId: Long): Long
}
