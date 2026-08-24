package kr.hanchae.moyeotrip.config.security

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import kr.hanchae.moyeotrip.exception.ErrorCode
import kr.hanchae.moyeotrip.exception.ErrorResponse
import kr.hanchae.moyeotrip.logging.SentryExceptionReporter
import kr.hanchae.moyeotrip.logging.TraceManager
import org.springframework.http.HttpStatus
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.stereotype.Component

@Component
class CustomAccessDeniedHandler(
    private val objectMapper: ObjectMapper,
    private val traceManager: TraceManager,
    private val sentryExceptionReporter: SentryExceptionReporter,
) : AccessDeniedHandler {
    override fun handle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        accessDeniedException: AccessDeniedException,
    ) {
        traceManager.doErrorLog(accessDeniedException)
        sentryExceptionReporter.capture(accessDeniedException, HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN.code)
        response.contentType = "application/json"
        response.status = HttpServletResponse.SC_FORBIDDEN
        objectMapper.writeValue(
            response.outputStream,
            ErrorResponse.of(
                ErrorCode.FORBIDDEN,
                ErrorCode.FORBIDDEN.errorMessage,
            ),
        )
    }
}
