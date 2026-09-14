package kr.hanchae.moyeotrip.service.terms

import kr.hanchae.moyeotrip.controller.terms.response.AgreementTermDetailResponse
import kr.hanchae.moyeotrip.controller.terms.response.AgreementTermSummaryResponse
import kr.hanchae.moyeotrip.entity.terms.AgreementTerm
import kr.hanchae.moyeotrip.entity.terms.AgreementTermCode
import kr.hanchae.moyeotrip.exception.BaseException
import kr.hanchae.moyeotrip.exception.ErrorCode
import kr.hanchae.moyeotrip.repository.AgreementTermRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class TermsService(
    private val agreementTermRepository: AgreementTermRepository,
) {
    /**
     * **가입 화면에 그릴 동의 항목**만 준다. 고지 문서(처리방침)는 여기 오지 않는다 —
     * 섞이면 가입 화면에 동의할 수 없는 체크박스가 생긴다.
     */
    fun getTerms(): List<AgreementTermSummaryResponse> =
        agreementTermRepository
            .findAllByActiveTrueOrderByIdAsc()
            .filter { it.code.consent }
            .map { it.toSummaryResponse() }

    /**
     * 한 종류 약관의 **현재 판본 본문**. 설정 화면이 쓴다 —
     * 클라이언트가 약관 본문을 하드코딩하면 서버 원장과 어긋나므로 종류만 알고 본문은 서버에서 받는다.
     */
    fun getLatestTerm(code: AgreementTermCode): AgreementTermDetailResponse =
        agreementTermRepository.findFirstByCodeAndActiveTrueOrderByIdDesc(code)?.toDetailResponse()
            ?: throw BaseException(ErrorCode.AGREEMENT_TERM_NOT_FOUND)

    fun getTerm(termId: Long): AgreementTermDetailResponse =
        agreementTermRepository.findByIdAndActiveTrue(termId)?.toDetailResponse()
            ?: throw BaseException(ErrorCode.AGREEMENT_TERM_NOT_FOUND)

    /**
     * 한 종류 약관의 **지난 판본까지** 최신순으로. 앱 약관 화면이
     * 「이전 판본은 설정 › 이용약관 › 지난 버전에서 볼 수 있어요」라고 적어 두고도 갈 곳이 없었다 (QA `BE-13`).
     *
     * 여기서는 `active` 를 보지 않는다 — **비활성 판본이야말로 이 목록의 존재 이유**다.
     * 판본이 하나뿐이면 그 하나만 온다(빈 목록이 아니다).
     */
    fun getTermHistory(code: AgreementTermCode): List<AgreementTermSummaryResponse> =
        agreementTermRepository
            .findAllByCodeOrderByIdDesc(code)
            .map { it.toSummaryResponse() }
            .ifEmpty { throw BaseException(ErrorCode.AGREEMENT_TERM_NOT_FOUND) }

    private fun AgreementTerm.toSummaryResponse() =
        AgreementTermSummaryResponse(
            termId = id,
            code = code,
            title = title,
            required = required,
            version = version,
        )

    private fun AgreementTerm.toDetailResponse() =
        AgreementTermDetailResponse(
            termId = id,
            code = code,
            title = title,
            required = required,
            version = version,
            content = content,
        )
}
