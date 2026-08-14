package kr.hanchae.moyeotrip.repository

import kr.hanchae.moyeotrip.entity.tour.TourismContentDetail
import org.springframework.data.jpa.repository.JpaRepository

interface TourismContentDetailRepository : JpaRepository<TourismContentDetail, Long> {
    fun findByTourismContentId(tourismContentId: Long): TourismContentDetail?
}
