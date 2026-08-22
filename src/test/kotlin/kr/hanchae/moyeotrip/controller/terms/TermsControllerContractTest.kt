package kr.hanchae.moyeotrip.controller.terms

import kr.hanchae.moyeotrip.controller.terms.response.AgreementTermDetailResponse
import kr.hanchae.moyeotrip.controller.terms.response.AgreementTermSummaryResponse
import kr.hanchae.moyeotrip.service.terms.TermsService
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class TermsControllerContractTest {
    private val termsService = mock(TermsService::class.java)
    private val mockMvc = MockMvcBuilders.standaloneSetup(TermsController(termsService)).build()
    private val markdownContent =
        """
        # 모여트립 이용약관

        ## 제1조 목적
        """.trimIndent()

    @Test
    fun `현재 회원가입 약관 목록을 반환한다`() {
        `when`(termsService.getTerms())
            .thenReturn(
                listOf(
                    AgreementTermSummaryResponse(
                        termId = 1L,
                        title = "[필수] 모여트립 이용약관",
                        required = true,
                    ),
                ),
            )

        mockMvc
            .perform(get("/api/v1/terms"))
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$[0].termId").value(1))
            .andExpect(jsonPath("$[0].required").value(true))
    }

    @Test
    fun `약관 상세는 Markdown 본문을 반환한다`() {
        `when`(termsService.getTerm(1L))
            .thenReturn(
                AgreementTermDetailResponse(
                    termId = 1L,
                    title = "[필수] 모여트립 이용약관",
                    required = true,
                    version = "2026.08.23",
                    content = markdownContent,
                ),
            )

        mockMvc
            .perform(get("/api/v1/terms/1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.termId").value(1))
            .andExpect(jsonPath("$.content").value(markdownContent))
    }
}
