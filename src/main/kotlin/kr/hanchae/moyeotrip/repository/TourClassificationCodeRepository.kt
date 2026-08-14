package kr.hanchae.moyeotrip.repository

import kr.hanchae.moyeotrip.entity.tour.TourClassificationCode
import org.springframework.data.jpa.repository.JpaRepository

interface TourClassificationCodeRepository : JpaRepository<TourClassificationCode, Long>
