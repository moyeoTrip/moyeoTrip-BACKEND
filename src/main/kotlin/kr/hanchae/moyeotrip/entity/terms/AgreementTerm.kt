package kr.hanchae.moyeotrip.entity.terms

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Lob
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import kr.hanchae.moyeotrip.entity.BaseTimeEntity

@Entity
@Table(
    name = "agreement_terms",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_agreement_term_code_ver", columnNames = ["term_code", "term_version"]),
    ],
)
class AgreementTerm(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
    @Enumerated(EnumType.STRING)
    @Column(name = "term_code", nullable = false, length = 50, updatable = false)
    val code: AgreementTermCode,
    @Column(nullable = false, length = 100, updatable = false)
    val title: String,
    @Column(name = "is_required", nullable = false, columnDefinition = "NUMBER(1)", updatable = false)
    val required: Boolean,
    @Lob
    @Column(nullable = false, updatable = false)
    val content: String,
    @Column(name = "term_version", nullable = false, length = 20, updatable = false)
    val version: String,
    @Column(nullable = false, columnDefinition = "NUMBER(1)")
    val active: Boolean = true,
) : BaseTimeEntity()

@Schema(
    description = "회원가입 약관 유형. SERVICE=서비스 이용약관, PRIVACY_COLLECTION=개인정보 수집·이용 동의, MARKETING=마케팅 정보 수신 동의",
    allowableValues = ["SERVICE", "PRIVACY_COLLECTION", "MARKETING"],
)
enum class AgreementTermCode {
    SERVICE,
    PRIVACY_COLLECTION,
    MARKETING,
}
