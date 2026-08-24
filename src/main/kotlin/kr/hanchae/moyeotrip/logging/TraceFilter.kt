package kr.hanchae.moyeotrip.logging

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterProperties
import org.springframework.core.Ordered
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.servlet.HandlerMapping
import org.springframework.web.util.ContentCachingRequestWrapper
import org.springframework.web.util.ContentCachingResponseWrapper
import java.time.Duration
import java.time.Instant
import java.util.UUID

// 응답 본문 캐시는 default off — ContentCachingResponseWrapper 가 size cap 없이 메모리 누적해 OOM 위험.
// 운영 디버깅 시에만 capture-response-body=true 로 opt-in.
@Component
class TraceFilter(
    private val traceManager: TraceManager,
    private val traceRepository: TraceRepository,
    private val sentryMetricsReporter: SentryMetricsRecorder,
) : OncePerRequestFilter(),
    Ordered {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val traceId = request.getHeader(TRACE_ID_HEADER)?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
        val startedAt = Instant.now()
        MDC.put(MDC_TRACE_ID, traceId)
        response.setHeader(TRACE_ID_HEADER, traceId)

        val wrappedRequest = ContentCachingRequestWrapper(request, MAX_PAYLOAD_LENGTH)
        val wrappedResponse = ContentCachingResponseWrapper(response)

        traceManager.wrappedRequest = wrappedRequest
        traceManager.wrappedResponse = wrappedResponse

        var unhandledThrowable: Throwable? = null
        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse)
        } catch (throwable: Throwable) {
            unhandledThrowable = throwable
            traceManager.doErrorLog(throwable)
            throw throwable
        } finally {
            val elapsedMillis = Duration.between(startedAt, Instant.now()).toMillis()
            try {
                if (traceManager.httpTrace == null) {
                    traceRepository.addTrace(
                        traceManager.getFilterTrace(
                            request = wrappedRequest,
                            response = wrappedResponse,
                            startedAt = startedAt,
                            elapsedMillis = elapsedMillis,
                        ),
                    )
                }
                sentryMetricsReporter.recordHttpResponse(
                    method = request.method,
                    route = resolveRoute(request),
                    statusCode = resolvedStatusCode(wrappedResponse.status, unhandledThrowable),
                    elapsedMillis = elapsedMillis,
                )
                wrappedResponse.copyBodyToResponse()
            } finally {
                MDC.remove(MDC_TRACE_ID)
            }
        }
    }

    // 요청 컨텍스트가 준비된 뒤, Spring Security 바로 앞에서 실행되어 401/403 응답까지 추적한다.
    override fun getOrder(): Int = SecurityFilterProperties.DEFAULT_FILTER_ORDER - 1

    private fun resolveRoute(request: HttpServletRequest): String =
        request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE) as? String ?: "unmatched"

    private fun resolvedStatusCode(
        responseStatus: Int,
        unhandledThrowable: Throwable?,
    ): Int =
        if (unhandledThrowable != null && responseStatus < HttpServletResponse.SC_BAD_REQUEST) {
            HttpServletResponse.SC_INTERNAL_SERVER_ERROR
        } else {
            responseStatus
        }

    companion object {
        const val MDC_TRACE_ID = "traceId"
        const val TRACE_ID_HEADER = "X-Trace-Id"
        const val MAX_PAYLOAD_LENGTH = 64 * 1024
    }
}
