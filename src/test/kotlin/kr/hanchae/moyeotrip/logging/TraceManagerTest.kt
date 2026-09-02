package kr.hanchae.moyeotrip.logging

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.boot.actuate.web.exchanges.HttpExchange
import org.springframework.web.util.ContentCachingRequestWrapper
import org.springframework.web.util.ContentCachingResponseWrapper
import java.net.URI
import java.time.Duration
import java.time.Instant

class TraceManagerTest {
    @Test
    fun `HTTP exchange가 없으면 trace를 만들지 않는다`() {
        assertNull(TraceManager().getTrace())
    }

    @Test
    fun `wrapper가 없으면 exchange 헤더와 기본값으로 trace를 만든다`() {
        val manager = TraceManager()
        manager.httpTrace =
            exchange(
                requestHeaders = mapOf("content-type" to listOf("application/json"), "Authorization" to listOf("Bearer secret")),
                responseHeaders = mapOf("CONTENT-TYPE" to listOf("application/json")),
                timeTaken = Duration.ofMillis(123),
            )

        val trace = manager.getTrace()!!

        assertEquals("/api/v1/test", trace.path)
        assertEquals("GET", trace.method)
        assertEquals(123, trace.elapsed)
        assertEquals("unknown", trace.remoteAddress)
        assertEquals(Redaction.REDACTED, trace.traceRequest.headers["Authorization"]?.single())
        assertTrue(trace.traceRequest.body.isEmpty())
        assertTrue(trace.traceResponse.body.isEmpty())
        assertNull(trace.cause)
        assertNull(trace.errorType)
        assertNull(trace.checkpoints)
        assertEquals(LogType.PROTOCOL, trace.logType)
        assertFalse(manager.isErrorLog())
    }

    @Test
    fun `wrapper 본문과 오류를 포함해 error trace를 만든다`() {
        val manager = TraceManager()
        val request = mock(ContentCachingRequestWrapper::class.java)
        val response = mock(ContentCachingResponseWrapper::class.java)
        `when`(request.contentType).thenReturn("application/json")
        `when`(request.contentAsByteArray).thenReturn("""{"password":"secret","name":"test"}""".toByteArray())
        `when`(request.remoteAddr).thenReturn("127.0.0.1")
        `when`(response.contentType).thenReturn("application/json")
        `when`(response.contentAsByteArray).thenReturn("""{"result":"ok"}""".toByteArray())
        manager.wrappedRequest = request
        manager.wrappedResponse = response
        manager.httpTrace = exchange(timeTaken = null)
        val exception = IllegalStateException("failed")
        manager.doErrorLog(exception)

        val trace = manager.getTrace()!!

        assertEquals(0, trace.elapsed)
        assertEquals("127.0.0.1", trace.remoteAddress)
        assertEquals(Redaction.REDACTED, trace.traceRequest.body["password"])
        assertEquals("test", trace.traceRequest.body["name"])
        assertEquals("ok", trace.traceResponse.body["result"])
        assertEquals(IllegalStateException::class.java, trace.errorType)
        assertNotNull(trace.cause)
        assertTrue(trace.cause!!.contains("failed"))
        assertEquals(LogType.ERROR, trace.logType)
        assertTrue(manager.isErrorLog())
    }

    @Test
    fun `throwable stack trace는 null과 예외를 구분한다`() {
        assertNull(TraceManager.throwableToStackTrace(null))
        val stackTrace = TraceManager.throwableToStackTrace(IllegalArgumentException("invalid"))
        assertTrue(stackTrace.orEmpty().contains("IllegalArgumentException"))
        assertTrue(stackTrace.orEmpty().contains("invalid"))
    }

    private fun exchange(
        requestHeaders: Map<String, List<String>> = emptyMap(),
        responseHeaders: Map<String, List<String>> = emptyMap(),
        timeTaken: Duration? = Duration.ofMillis(10),
    ): HttpExchange =
        HttpExchange(
            Instant.parse("2026-08-30T00:00:00Z"),
            HttpExchange.Request(URI("https://example.com/api/v1/test?keyword=test"), "127.0.0.1", "GET", requestHeaders),
            HttpExchange.Response(200, responseHeaders),
            null,
            null,
            timeTaken,
        )
}
