package kr.hanchae.moyeotrip.repository

import kr.hanchae.moyeotrip.entity.user.TravelStyle
import org.springframework.data.jpa.repository.JpaRepository

interface TravelStyleRepository : JpaRepository<TravelStyle, Long> {
    fun findAllByOrderByLabelAsc(): List<TravelStyle>
}
