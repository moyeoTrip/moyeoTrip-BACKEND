package kr.hanchae.moyeotrip.repository

import kr.hanchae.moyeotrip.entity.terms.AgreementTerm
import kr.hanchae.moyeotrip.entity.terms.AgreementTermCode
import org.springframework.data.jpa.repository.JpaRepository

interface AgreementTermRepository : JpaRepository<AgreementTerm, Long> {
    fun findAllByActiveTrueOrderByIdAsc(): List<AgreementTerm>

    fun findByIdAndActiveTrue(id: Long): AgreementTerm?

    /**
     * 판본 이력. **`active` 를 보지 않는다** — 지난 판본(비활성)이야말로 이 조회의 목적이다.
     * 최신 판본이 먼저 오도록 id 역순으로 준다.
     */
    fun findAllByCodeOrderByIdDesc(code: AgreementTermCode): List<AgreementTerm>

    /**
     * 한 종류 약관의 **현재 판본**. 설정 화면은 `termId` 를 모르고 종류만 안다 —
     * `termId` 는 판본이 바뀔 때마다 새로 발급되므로 클라이언트가 들고 있을 수 없다.
     */
    fun findFirstByCodeAndActiveTrueOrderByIdDesc(code: AgreementTermCode): AgreementTerm?
}
