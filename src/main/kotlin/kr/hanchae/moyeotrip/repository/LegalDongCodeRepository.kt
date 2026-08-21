package kr.hanchae.moyeotrip.repository

import kr.hanchae.moyeotrip.entity.tour.LegalDongCode
import org.springframework.data.jpa.repository.JpaRepository

interface LegalDongCodeRepository : JpaRepository<LegalDongCode, Long> {
    fun findAllByRegionCode(regionCode: String): List<LegalDongCode>

    fun findAllByRegionCodeOrderBySignguNameAsc(regionCode: String): List<LegalDongCode>

    fun findAllByRegionCodeAndSignguCodeIn(
        regionCode: String,
        signguCodes: Collection<String>,
    ): List<LegalDongCode>
}
