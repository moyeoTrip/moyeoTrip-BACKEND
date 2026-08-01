package kr.hanchae.moyeotrip.config.security

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kr.hanchae.moyeotrip.exception.ErrorCode
import kr.hanchae.moyeotrip.logging.TraceFilter
import kr.hanchae.moyeotrip.logging.TraceManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterProperties
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.InsufficientAuthenticationException

class SecurityTraceHandlerTest {
    private val request = MockHttpServletRequest()

    @Test
    fun `401 인증 실패를 TraceManager에 오류로 등록한다`() {
        val traceManager = mock(TraceManager::class.java)
        val response = MockHttpServletResponse()
        val exception = InsufficientAuthenticationException("authentication required")
        val entryPoint = CustomAuthenticationEntryPoint(jacksonObjectMapper(), traceManager)

        entryPoint.commence(request, response, exception)

        assertEquals(401, response.status)
        assertTrue(response.contentAsString.contains(ErrorCode.UNAUTHORIZED.code.toString()))
        verify(traceManager).doErrorLog(exception)
    }

    @Test
    fun `403 권한 거부를 TraceManager에 오류로 등록한다`() {
        val traceManager = mock(TraceManager::class.java)
        val response = MockHttpServletResponse()
        val exception = AccessDeniedException("access denied")
        val handler = CustomAccessDeniedHandler(jacksonObjectMapper(), traceManager)

        handler.handle(request, response, exception)

        assertEquals(403, response.status)
        assertTrue(response.contentAsString.contains(ErrorCode.FORBIDDEN.code.toString()))
        verify(traceManager).doErrorLog(exception)
    }

    @Test
    fun `TraceFilter는 Spring Security보다 먼저 실행한다`() {
        val filter = TraceFilter(mock(TraceManager::class.java))

        assertEquals(SecurityFilterProperties.DEFAULT_FILTER_ORDER - 1, filter.order)
    }
}
