package kr.hanchae.moyeotrip.config.security

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import kr.hanchae.moyeotrip.exception.ErrorCode
import kr.hanchae.moyeotrip.exception.ErrorResponse
import kr.hanchae.moyeotrip.logging.SentryExceptionReporter
import kr.hanchae.moyeotrip.logging.TraceManager
import org.springframework.http.HttpStatus
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.stereotype.Component

@Component
class CustomAuthenticationEntryPoint(
    private val objectMapper: ObjectMapper,
    private val traceManager: TraceManager,
    private val sentryExceptionReporter: SentryExceptionReporter,
) : AuthenticationEntryPoint {
    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException,
    ) {
        traceManager.doErrorLog(authException)
        sentryExceptionReporter.capture(authException, HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED.code)
        response.contentType = "application/json"
        response.status = HttpServletResponse.SC_UNAUTHORIZED
        objectMapper.writeValue(
            response.outputStream,
            ErrorResponse.of(
                ErrorCode.UNAUTHORIZED,
                ErrorCode.UNAUTHORIZED.errorMessage,
            ),
        )
    }
}
