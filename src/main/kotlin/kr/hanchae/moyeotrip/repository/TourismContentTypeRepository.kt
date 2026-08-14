package kr.hanchae.moyeotrip.repository

import kr.hanchae.moyeotrip.entity.tour.TourismContentType
import org.springframework.data.jpa.repository.JpaRepository

interface TourismContentTypeRepository : JpaRepository<TourismContentType, Int> {
    fun findAllByCodeNotOrderByCodeAsc(code: Int): List<TourismContentType>
}
