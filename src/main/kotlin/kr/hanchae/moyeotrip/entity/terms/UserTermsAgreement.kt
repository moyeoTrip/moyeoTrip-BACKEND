package kr.hanchae.moyeotrip.entity.terms

import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import kr.hanchae.moyeotrip.entity.BaseTimeEntity
import kr.hanchae.moyeotrip.entity.user.User

@Entity
@Table(
    name = "user_terms_agreements",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_user_term_agreement", columnNames = ["user_id", "agreement_term_id"]),
    ],
)
class UserTermsAgreement(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    val user: User,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agreement_term_id", nullable = false, updatable = false)
    val agreementTerm: AgreementTerm,
) : BaseTimeEntity()
