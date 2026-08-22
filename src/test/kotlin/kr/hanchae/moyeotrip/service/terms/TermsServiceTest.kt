package kr.hanchae.moyeotrip.service.terms

import kr.hanchae.moyeotrip.entity.terms.AgreementTerm
import kr.hanchae.moyeotrip.entity.terms.AgreementTermCode
import kr.hanchae.moyeotrip.exception.BaseException
import kr.hanchae.moyeotrip.exception.ErrorCode
import kr.hanchae.moyeotrip.repository.AgreementTermRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class TermsServiceTest {
    private lateinit var agreementTermRepository: AgreementTermRepository
    private lateinit var termsService: TermsService

    @BeforeEach
    fun setUp() {
        agreementTermRepository = mock(AgreementTermRepository::class.java)
        termsService = TermsService(agreementTermRepository)
    }

    @Nested
    inner class GetTerms {
        @Test
        fun `활성 약관의 ID 제목 필수 여부만 목록으로 반환한다`() {
            `when`(agreementTermRepository.findAllByActiveTrueOrderByIdAsc())
                .thenReturn(
                    listOf(
                        term(id = 1L, required = true),
                        term(id = 3L, required = false, code = AgreementTermCode.MARKETING),
                    ),
                )

            val response = termsService.getTerms()

            assertEquals(listOf(1L, 3L), response.map { it.termId })
            assertEquals(listOf("[필수] 모여트립 이용약관", "[선택] 마케팅 정보 수신 동의"), response.map { it.title })
            assertEquals(listOf(true, false), response.map { it.required })
        }
    }

    @Nested
    inner class GetTerm {
        @Test
        fun `활성 약관 상세는 Markdown 본문과 버전을 반환한다`() {
            val agreementTerm = term(id = 1L, required = true)
            `when`(agreementTermRepository.findByIdAndActiveTrue(1L)).thenReturn(agreementTerm)

            val response = termsService.getTerm(1L)

            assertEquals(1L, response.termId)
            assertEquals("2026.08.23", response.version)
            assertEquals("# 모여트립 이용약관", response.content)
        }

        @Test
        fun `비활성 또는 없는 약관은 조회할 수 없다`() {
            `when`(agreementTermRepository.findByIdAndActiveTrue(99L)).thenReturn(null)

            val exception = assertThrows(BaseException::class.java) { termsService.getTerm(99L) }

            assertEquals(ErrorCode.AGREEMENT_TERM_NOT_FOUND, exception.errorCode)
        }
    }

    private fun term(
        id: Long,
        required: Boolean,
        code: AgreementTermCode = AgreementTermCode.SERVICE,
    ): AgreementTerm =
        AgreementTerm(
            id = id,
            code = code,
            title = if (required) "[필수] 모여트립 이용약관" else "[선택] 마케팅 정보 수신 동의",
            required = required,
            content = "# 모여트립 이용약관",
            version = "2026.08.23",
        )
}
