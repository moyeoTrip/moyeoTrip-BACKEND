package kr.hanchae.moyeotrip.exception

import kr.hanchae.moyeotrip.logging.SentryExceptionReporter
import kr.hanchae.moyeotrip.logging.TraceManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.servlet.resource.NoResourceFoundException

class GlobalExceptionHandlerTest {
    private val traceManager = mock(TraceManager::class.java)
    private val sentryExceptionReporter = mock(SentryExceptionReporter::class.java)
    private val handler = GlobalExceptionHandler(traceManager, sentryExceptionReporter)

    @Test
    fun `존재하지 않는 API 경로는 404를 반환한다`() {
        val exception = mock(NoResourceFoundException::class.java)

        val response = handler.handleNoResourceFound(exception)

        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
        assertEquals(MediaType.APPLICATION_JSON, response.headers.contentType)
        assertEquals(ErrorCode.RESOURCE_NOT_FOUND.code, response.body?.code)
        assertEquals(ErrorCode.RESOURCE_NOT_FOUND.errorMessage, response.body?.errorMessage)
        verify(traceManager).doErrorLog(exception)
        verifyNoInteractions(sentryExceptionReporter)
    }

    @Test
    fun `예상하지 못한 서버 예외는 Sentry에 전송한다`() {
        val exception = IllegalStateException("unexpected")

        val response = handler.handleUnexpectedException(exception)

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
        assertEquals(ErrorCode.INTERNAL_SERVER_ERROR.code, response.body?.code)
        verify(sentryExceptionReporter)
            .capture(exception, HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_SERVER_ERROR.code)
        verify(traceManager).doErrorLog(exception)
    }

    @Test
    fun `400 BaseException은 Sentry에 전송한다`() {
        val exception = BaseException(ErrorCode.BAD_REQUEST, "invalid request")

        val response = handler.handleBaseException(exception)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        verify(sentryExceptionReporter).capture(exception, HttpStatus.BAD_REQUEST, ErrorCode.BAD_REQUEST.code)
        verify(traceManager).doErrorLog(exception)
    }
}
