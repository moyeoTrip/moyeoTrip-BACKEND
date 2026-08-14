package kr.hanchae.moyeotrip.repository

import kr.hanchae.moyeotrip.entity.tour.TourismContent
import org.springframework.data.jpa.repository.JpaRepository

interface TourismContentRepository : JpaRepository<TourismContent, Long>
