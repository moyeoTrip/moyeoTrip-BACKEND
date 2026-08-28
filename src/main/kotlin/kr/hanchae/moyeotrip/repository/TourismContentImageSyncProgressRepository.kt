package kr.hanchae.moyeotrip.repository

import kr.hanchae.moyeotrip.entity.tour.TourismContentImageSyncProgress
import org.springframework.data.jpa.repository.JpaRepository

interface TourismContentImageSyncProgressRepository : JpaRepository<TourismContentImageSyncProgress, String>
