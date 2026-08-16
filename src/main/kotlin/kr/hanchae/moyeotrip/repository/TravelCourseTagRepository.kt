package kr.hanchae.moyeotrip.repository

import kr.hanchae.moyeotrip.entity.tour.TravelCourseTag
import org.springframework.data.jpa.repository.JpaRepository

interface TravelCourseTagRepository : JpaRepository<TravelCourseTag, Long> {
    fun findAllByOrderByIdAsc(): List<TravelCourseTag>
}
