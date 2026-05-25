package kr.hanchae.moyeotrip.logging

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.core.Ordered
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.util.ContentCachingRequestWrapper
import org.springframework.web.util.ContentCachingResponseWrapper
import java.util.UUID

// 응답 본문 캐시는 default off — ContentCachingResponseWrapper 가 size cap 없이 메모리 누적해 OOM 위험.
// 운영 디버깅 시에만 capture-response-body=true 로 opt-in.
@Component
class TraceFilter(
    private val traceManager: TraceManager,
) : OncePerRequestFilter(),
    Ordered {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val traceId = request.getHeader(TRACE_ID_HEADER)?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
        MDC.put(MDC_TRACE_ID, traceId)
        response.setHeader(TRACE_ID_HEADER, traceId)

        val wrappedRequest = ContentCachingRequestWrapper(request, MAX_PAYLOAD_LENGTH)
        val wrappedResponse = ContentCachingResponseWrapper(response)

        traceManager.wrappedRequest = wrappedRequest
        traceManager.wrappedResponse = wrappedResponse

        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse ?: response)
        } finally {
            try {
                wrappedResponse.copyBodyToResponse()
            } finally {
                MDC.remove(MDC_TRACE_ID)
            }
        }
    }

    override fun getOrder(): Int = Ordered.LOWEST_PRECEDENCE - 11 // precedence HttpTraceFilter

    companion object {
        const val MDC_TRACE_ID = "traceId"
        const val TRACE_ID_HEADER = "X-Trace-Id"
        const val MAX_PAYLOAD_LENGTH = 64 * 1024
    }
}
