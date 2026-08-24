package kr.hanchae.moyeotrip.config.security

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kr.hanchae.moyeotrip.exception.ErrorCode
import kr.hanchae.moyeotrip.logging.SentryExceptionReporter
import kr.hanchae.moyeotrip.logging.SentryMetricsRecorder
import kr.hanchae.moyeotrip.logging.TraceFilter
import kr.hanchae.moyeotrip.logging.TraceManager
import kr.hanchae.moyeotrip.logging.TraceRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterProperties
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.InsufficientAuthenticationException
import java.nio.charset.StandardCharsets

class SecurityTraceHandlerTest {
    private val request = MockHttpServletRequest()

    @Test
    fun `401 인증 실패를 TraceManager에 오류로 등록한다`() {
        val traceManager = mock(TraceManager::class.java)
        val sentryExceptionReporter = mock(SentryExceptionReporter::class.java)
        val response = MockHttpServletResponse()
        val exception = InsufficientAuthenticationException("authentication required")
        val entryPoint = CustomAuthenticationEntryPoint(jacksonObjectMapper(), traceManager, sentryExceptionReporter)

        entryPoint.commence(request, response, exception)

        assertEquals(401, response.status)
        assertTrue(response.contentAsString.contains(ErrorCode.UNAUTHORIZED.code.toString()))
        verify(traceManager).doErrorLog(exception)
        verify(sentryExceptionReporter).capture(exception, HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED.code)
    }

    @Test
    fun `403 권한 거부를 TraceManager에 오류로 등록한다`() {
        val traceManager = mock(TraceManager::class.java)
        val sentryExceptionReporter = mock(SentryExceptionReporter::class.java)
        val response = MockHttpServletResponse()
        val exception = AccessDeniedException("access denied")
        val handler = CustomAccessDeniedHandler(jacksonObjectMapper(), traceManager, sentryExceptionReporter)

        handler.handle(request, response, exception)

        assertEquals(403, response.status)
        assertTrue(response.contentAsString.contains(ErrorCode.FORBIDDEN.code.toString()))
        verify(traceManager).doErrorLog(exception)
        verify(sentryExceptionReporter).capture(exception, HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN.code)
    }

    @Test
    fun `TraceFilter는 Spring Security보다 먼저 실행한다`() {
        val traceManager = mock(TraceManager::class.java)
        val filter = TraceFilter(traceManager, mock(TraceRepository::class.java), mock(SentryMetricsRecorder::class.java))

        assertEquals(SecurityFilterProperties.DEFAULT_FILTER_ORDER - 1, filter.order)
    }

    @Test
    fun `Actuator HttpExchange가 생성되지 않은 401도 fallback trace로 기록한다`() {
        val traceManager = TraceManager()
        val traceRepository = TraceRepository(traceManager)
        val filter = TraceFilter(traceManager, traceRepository, mock(SentryMetricsRecorder::class.java))
        val response = MockHttpServletResponse()
        request.method = "GET"
        request.requestURI = "/api/v1/users/me"

        filter.doFilter(request, response) { _, servletResponse ->
            traceManager.doErrorLog(InsufficientAuthenticationException("authentication required"))
            (servletResponse as jakarta.servlet.http.HttpServletResponse).apply {
                status = 401
                contentType = "application/json"
                outputStream.write("{\"code\":40100}".toByteArray(StandardCharsets.UTF_8))
            }
        }

        val trace = traceRepository.findAllTrace().single()
        assertEquals(401, trace.traceResponse.status)
        assertEquals("/api/v1/users/me", trace.path)
        assertTrue(trace.cause?.contains("authentication required") == true)
        assertTrue(response.contentAsString.contains("40100"))
    }

    @Test
    fun `HTTP 오류 응답을 Sentry 메트릭으로 기록한다`() {
        val traceManager = TraceManager()
        val traceRepository = TraceRepository(traceManager)
        val sentryMetricsReporter = RecordingSentryMetricsRecorder()
        val filter = TraceFilter(traceManager, traceRepository, sentryMetricsReporter)
        val response = MockHttpServletResponse()
        request.method = "GET"
        request.requestURI = "/api/v1/missing"

        filter.doFilter(request, response) { _, servletResponse ->
            (servletResponse as jakarta.servlet.http.HttpServletResponse).status = 404
        }

        assertEquals(listOf(HttpResponseMetric("GET", "unmatched", 404)), sentryMetricsReporter.metrics)
    }

    @Test
    fun `Kubernetes Actuator health probe 경로는 인증 없이 허용한다`() {
        assertTrue(PERMITTED_URL_PATTERNS.contains("/actuator/health"))
        assertTrue(PERMITTED_URL_PATTERNS.contains("/actuator/health/**"))
    }

    @Test
    fun `회원가입 전 약관 조회 경로는 인증 없이 허용한다`() {
        assertTrue(PERMITTED_URL_PATTERNS.contains("/api/v1/terms/**"))
    }

    private class RecordingSentryMetricsRecorder : SentryMetricsRecorder {
        val metrics = mutableListOf<HttpResponseMetric>()

        override fun recordHttpResponse(
            method: String,
            route: String,
            statusCode: Int,
            elapsedMillis: Long,
        ) {
            metrics += HttpResponseMetric(method, route, statusCode)
        }
    }

    private data class HttpResponseMetric(
        val method: String,
        val route: String,
        val statusCode: Int,
    )
}
