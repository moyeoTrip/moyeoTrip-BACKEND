package kr.hanchae.moyeotrip.logging

import io.sentry.Sentry
import org.slf4j.MDC
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component

@Component
class SentryExceptionReporter {
    fun capture(
        exception: Throwable,
        httpStatus: HttpStatus,
        errorCode: Int,
    ) = capture(
        exception,
        mapOf(
            "http.status_code" to httpStatus.value().toString(),
            "error.code" to errorCode.toString(),
        ),
    )

    fun capture(
        exception: Throwable,
        tags: Map<String, String>,
    ) {
        Sentry.captureException(exception) { scope ->
            MDC.get(TraceFilter.MDC_TRACE_ID)?.let { scope.setTag(TraceFilter.MDC_TRACE_ID, it) }
            tags.forEach(scope::setTag)
        }
    }
}
