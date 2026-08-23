package kr.hanchae.moyeotrip.logging

import io.sentry.Sentry
import io.sentry.metrics.MetricsUnit
import io.sentry.metrics.SentryMetricsParameters
import org.springframework.stereotype.Component

interface SentryMetricsRecorder {
    fun recordHttpResponse(
        method: String,
        route: String,
        statusCode: Int,
        elapsedMillis: Long,
    )
}

@Component
class SentryMetricsReporter : SentryMetricsRecorder {
    override fun recordHttpResponse(
        method: String,
        route: String,
        statusCode: Int,
        elapsedMillis: Long,
    ) {
        val attributes =
            mapOf(
                "http.request.method" to method,
                "http.route" to route,
                "http.response.status_code" to statusCode.toString(),
            )
        val params = SentryMetricsParameters.create(attributes)

        Sentry.metrics().count(HTTP_REQUEST_COUNT, 1.0, null, params)
        Sentry
            .metrics()
            .distribution(
                HTTP_REQUEST_DURATION,
                elapsedMillis.coerceAtLeast(0).toDouble(),
                MetricsUnit.Duration.MILLISECOND,
                params,
            )
        if (statusCode >= 400) {
            Sentry.metrics().count(HTTP_ERROR_COUNT, 1.0, null, params)
        }
    }

    companion object {
        const val HTTP_REQUEST_COUNT = "moyeotrip.http.requests"
        const val HTTP_REQUEST_DURATION = "moyeotrip.http.server.duration"
        const val HTTP_ERROR_COUNT = "moyeotrip.http.errors"
    }
}
