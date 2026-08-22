package kr.hanchae.moyeotrip.service.terms

import kr.hanchae.moyeotrip.controller.terms.response.AgreementTermDetailResponse
import kr.hanchae.moyeotrip.controller.terms.response.AgreementTermSummaryResponse
import kr.hanchae.moyeotrip.entity.terms.AgreementTerm
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
    fun getTerms(): List<AgreementTermSummaryResponse> =
        agreementTermRepository.findAllByActiveTrueOrderByIdAsc().map { it.toSummaryResponse() }

    fun getTerm(termId: Long): AgreementTermDetailResponse =
        agreementTermRepository.findByIdAndActiveTrue(termId)?.toDetailResponse()
            ?: throw BaseException(ErrorCode.AGREEMENT_TERM_NOT_FOUND)

    private fun AgreementTerm.toSummaryResponse() =
        AgreementTermSummaryResponse(
            termId = id,
            title = title,
            required = required,
        )

    private fun AgreementTerm.toDetailResponse() =
        AgreementTermDetailResponse(
            termId = id,
            title = title,
            required = required,
            version = version,
            content = content,
        )
}
