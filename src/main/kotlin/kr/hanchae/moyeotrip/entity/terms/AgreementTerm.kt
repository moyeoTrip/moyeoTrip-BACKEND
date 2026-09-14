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

/**
 * 약관 종류를 가리키는 **안정적인 식별자**.
 *
 * 이 값을 응답에 내보내지 않았던 탓에, 세 클라이언트가 **제목 문자열로 약관을 찾고 있었다**
 * (`title.contains("개인정보")` 같은 식). 제목이 조금만 바뀌어도 엉뚱한 약관이 열리거나
 * 아무것도 안 열린다 — 실제로 처리방침을 열면 **가입 동의서가 대신 열렸다**(QA `BE-03`).
 * `termId` 는 판본마다 새로 발급되므로 「이 종류의 약관」을 가리키는 데는 쓸 수 없다.
 */
@Schema(
    description =
        "약관 종류. SERVICE=서비스 이용약관, PRIVACY_COLLECTION=개인정보 수집·이용 동의, " +
            "PRIVACY_POLICY=개인정보 처리방침, MARKETING=마케팅 정보 수신 동의",
    allowableValues = ["SERVICE", "PRIVACY_COLLECTION", "PRIVACY_POLICY", "MARKETING"],
)
enum class AgreementTermCode(
    /**
     * 가입할 때 **동의를 받는 문서**인지. 가입 약관 목록(`GET /api/v1/terms`)은 이 값이 `true` 인 것만 준다.
     *
     * `required = false` 로는 구분할 수 없다 — 마케팅 수신 동의도 선택이지만 **체크박스로 동의를 받는다**.
     * 고지 문서인 처리방침이 그 목록에 섞이면 가입 화면에 「[안내] 개인정보 처리방침」 체크박스가 생긴다.
     */
    val consent: Boolean,
) {
    SERVICE(consent = true),
    PRIVACY_COLLECTION(consent = true),

    /**
     * 개인정보 **처리방침**. 수집·이용 **동의서**(`PRIVACY_COLLECTION`)와 다른 문서다 —
     * 동의 대상이 아니라 고지 문서라 가입 목록에 넣지 않는다. 설정 화면이 `GET /api/v1/terms/latest/{code}` 로 직접 가져간다.
     */
    PRIVACY_POLICY(consent = false),
    MARKETING(consent = true),
}
