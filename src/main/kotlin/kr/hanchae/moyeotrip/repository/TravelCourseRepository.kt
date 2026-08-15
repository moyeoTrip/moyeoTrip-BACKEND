package kr.hanchae.moyeotrip.repository

import kr.hanchae.moyeotrip.entity.tour.TravelCourse
import kr.hanchae.moyeotrip.entity.tour.TravelCourseType
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface TravelCourseRepository : JpaRepository<TravelCourse, Long> {
    fun findByIdAndType(
        id: Long,
        type: TravelCourseType,
    ): TravelCourse?

    fun findAllByTypeOrderByCreatedDateTimeDesc(type: TravelCourseType): List<TravelCourse>

    fun existsByTypeAndTitle(
        type: TravelCourseType,
        title: String,
    ): Boolean

    @Query(
        """
        SELECT course FROM TravelCourse course
        LEFT JOIN ChatRoom room ON room.course = course
        WHERE course.type = PUBLIC
        GROUP BY course
        ORDER BY COUNT(room) DESC, course.id DESC
        """,
    )
    fun findPopularPublicCourses(pageable: Pageable): List<TravelCourse>
}
