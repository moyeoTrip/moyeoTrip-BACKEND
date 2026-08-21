package kr.hanchae.moyeotrip.repository

import kr.hanchae.moyeotrip.entity.tour.TravelCourse
import kr.hanchae.moyeotrip.entity.tour.TravelCourseType
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface TravelCourseRepository : JpaRepository<TravelCourse, Long> {
    fun findByIdAndType(
        id: Long,
        type: TravelCourseType,
    ): TravelCourse?

    fun findAllByTypeOrderByCreatedDateTimeDesc(type: TravelCourseType): List<TravelCourse>

    @Query(
        """
        SELECT DISTINCT course FROM TravelCourse course
        JOIN course.courseTags tag
        WHERE course.type = :type AND tag.id = :tagId
        ORDER BY course.createdDateTime DESC
        """,
    )
    fun findAllByTypeAndTagIdOrderByCreatedDateTimeDesc(
        @Param("type") type: TravelCourseType,
        @Param("tagId") tagId: Long,
    ): List<TravelCourse>

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
