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

        @Test
        fun `고지 문서인 처리방침은 가입 동의 목록에 오지 않는다`() {
            // 처리방침은 동의를 받는 문서가 아니다. 섞이면 가입 화면에 동의할 수 없는 체크박스가 생긴다.
            `when`(agreementTermRepository.findAllByActiveTrueOrderByIdAsc())
                .thenReturn(
                    listOf(
                        term(id = 1L, required = true),
                        term(id = 4L, required = false, code = AgreementTermCode.PRIVACY_POLICY),
                    ),
                )

            val response = termsService.getTerms()

            assertEquals(listOf(1L), response.map { it.termId })
        }
    }

    @Nested
    inner class GetLatestTerm {
        @Test
        fun `종류로 현재 판본 본문을 가져온다`() {
            `when`(agreementTermRepository.findFirstByCodeAndActiveTrueOrderByIdDesc(AgreementTermCode.PRIVACY_POLICY))
                .thenReturn(term(id = 4L, required = false, code = AgreementTermCode.PRIVACY_POLICY))

            val response = termsService.getLatestTerm(AgreementTermCode.PRIVACY_POLICY)

            assertEquals(AgreementTermCode.PRIVACY_POLICY, response.code)
            assertEquals("# 모여트립 이용약관", response.content)
        }

        @Test
        fun `그 종류의 활성 판본이 없으면 조회할 수 없다`() {
            `when`(agreementTermRepository.findFirstByCodeAndActiveTrueOrderByIdDesc(AgreementTermCode.PRIVACY_POLICY))
                .thenReturn(null)

            val exception =
                assertThrows(BaseException::class.java) {
                    termsService.getLatestTerm(AgreementTermCode.PRIVACY_POLICY)
                }

            assertEquals(ErrorCode.AGREEMENT_TERM_NOT_FOUND, exception.errorCode)
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
