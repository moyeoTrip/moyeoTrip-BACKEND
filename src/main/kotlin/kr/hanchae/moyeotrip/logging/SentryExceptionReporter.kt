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
    ) {
        Sentry.captureException(exception) { scope ->
            MDC.get(TraceFilter.MDC_TRACE_ID)?.let { scope.setTag(TraceFilter.MDC_TRACE_ID, it) }
            scope.setTag("http.status_code", httpStatus.value().toString())
            scope.setTag("error.code", errorCode.toString())
        }
    }
}
