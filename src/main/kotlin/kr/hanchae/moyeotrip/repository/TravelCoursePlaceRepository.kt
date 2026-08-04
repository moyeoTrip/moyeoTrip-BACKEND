package kr.hanchae.moyeotrip.repository

import kr.hanchae.moyeotrip.entity.chat.TravelCoursePlace
import org.springframework.data.jpa.repository.JpaRepository

interface TravelCoursePlaceRepository : JpaRepository<TravelCoursePlace, Long>
