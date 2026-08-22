package kr.hanchae.moyeotrip.controller.terms

import kr.hanchae.moyeotrip.controller.terms.response.AgreementTermDetailResponse
import kr.hanchae.moyeotrip.controller.terms.response.AgreementTermSummaryResponse
import kr.hanchae.moyeotrip.service.terms.TermsService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/terms")
class TermsController(
    private val termsService: TermsService,
) : TermsAPISpec {
    @GetMapping
    override fun getTerms(): List<AgreementTermSummaryResponse> = termsService.getTerms()

    @GetMapping("/{termId}")
    override fun getTerm(
        @PathVariable termId: Long,
    ): AgreementTermDetailResponse = termsService.getTerm(termId)
}
