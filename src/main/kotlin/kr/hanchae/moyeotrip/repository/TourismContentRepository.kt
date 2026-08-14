package kr.hanchae.moyeotrip.repository

import kr.hanchae.moyeotrip.entity.tour.TourismContent
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface TourismContentRepository : JpaRepository<TourismContent, Long> {
    fun findByContentId(contentId: Long): TourismContent?

    fun findAllByContentTypeCode(
        contentTypeCode: Int,
        pageable: Pageable,
    ): Page<TourismContent>

    fun findAllByContentTypeCodeNot(
        contentTypeCode: Int,
        pageable: Pageable,
    ): Page<TourismContent>
}
