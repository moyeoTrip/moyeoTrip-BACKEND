package kr.hanchae.moyeotrip.repository

import kr.hanchae.moyeotrip.entity.terms.UserTermsAgreement
import org.springframework.data.jpa.repository.JpaRepository

interface UserTermsAgreementRepository : JpaRepository<UserTermsAgreement, Long> {
    fun findAllByUserIdAndAgreementTermIdIn(
        userId: Long,
        agreementTermIds: Set<Long>,
    ): List<UserTermsAgreement>
}
