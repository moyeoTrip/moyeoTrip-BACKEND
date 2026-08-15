package kr.hanchae.moyeotrip.repository

import kr.hanchae.moyeotrip.entity.tour.TourismContentImage
import kr.hanchae.moyeotrip.entity.tour.TourismContentImageType
import org.springframework.data.jpa.repository.JpaRepository

interface TourismContentImageRepository : JpaRepository<TourismContentImage, Long> {
    fun findAllByTourismContentIdAndTypeOrderByIdAsc(
        tourismContentId: Long,
        type: TourismContentImageType,
    ): List<TourismContentImage>
}
