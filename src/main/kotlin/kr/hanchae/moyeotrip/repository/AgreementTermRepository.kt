package kr.hanchae.moyeotrip.repository

import kr.hanchae.moyeotrip.entity.terms.AgreementTerm
import org.springframework.data.jpa.repository.JpaRepository

interface AgreementTermRepository : JpaRepository<AgreementTerm, Long> {
    fun findAllByActiveTrueOrderByIdAsc(): List<AgreementTerm>

    fun findByIdAndActiveTrue(id: Long): AgreementTerm?
}
