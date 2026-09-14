package kr.hanchae.moyeotrip.controller.terms

import kr.hanchae.moyeotrip.controller.terms.response.AgreementTermDetailResponse
import kr.hanchae.moyeotrip.controller.terms.response.AgreementTermSummaryResponse
import kr.hanchae.moyeotrip.entity.terms.AgreementTermCode
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

    // 종류로 현재 판본 본문을 바로 준다. 설정 화면은 `termId` 를 알 수 없다(판본마다 바뀐다).
    @GetMapping("/latest/{code}")
    override fun getLatestTerm(
        @PathVariable code: AgreementTermCode,
    ): AgreementTermDetailResponse = termsService.getLatestTerm(code)

    // `{termId}` 와 겹치지 않게 `history` 를 앞에 둔다 — `termId` 는 숫자라 충돌하지 않지만
    // 경로를 읽는 사람에게 「이건 id 가 아니라 종류」라는 것이 드러나야 한다.
    @GetMapping("/history/{code}")
    override fun getTermHistory(
        @PathVariable code: AgreementTermCode,
    ): List<AgreementTermSummaryResponse> = termsService.getTermHistory(code)
}
