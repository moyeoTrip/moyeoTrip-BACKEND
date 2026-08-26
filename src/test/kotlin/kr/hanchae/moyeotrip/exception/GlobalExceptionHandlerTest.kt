package kr.hanchae.moyeotrip.exception

import com.fasterxml.jackson.core.JsonLocation
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException
import kr.hanchae.moyeotrip.logging.SentryExceptionReporter
import kr.hanchae.moyeotrip.logging.TraceManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.mock.http.MockHttpInputMessage
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.multipart.support.MissingServletRequestPartException
import org.springframework.web.servlet.resource.NoResourceFoundException

class GlobalExceptionHandlerTest {
    private val traceManager = mock(TraceManager::class.java)
    private val sentryExceptionReporter = mock(SentryExceptionReporter::class.java)
    private val handler = GlobalExceptionHandler(traceManager, sentryExceptionReporter)

    @Test
    fun `지원하지 않는 HTTP 메서드는 405를 반환한다`() {
        val exception = HttpRequestMethodNotSupportedException("PATCH")

        val response = handler.handleMethodNotSupported(exception)

        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, response.statusCode)
        assertEquals(ErrorCode.METHOD_NOT_ALLOWED.code, response.body?.code)
        verify(traceManager).doErrorLog(exception)
    }

    @Test
    fun `존재하지 않는 API 경로는 404를 반환한다`() {
        val exception = mock(NoResourceFoundException::class.java)

        val response = handler.handleNoResourceFound(exception)

        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
        assertEquals(MediaType.APPLICATION_JSON, response.headers.contentType)
        assertEquals(ErrorCode.RESOURCE_NOT_FOUND.code, response.body?.code)
        assertEquals(ErrorCode.RESOURCE_NOT_FOUND.errorMessage, response.body?.errorMessage)
        verify(traceManager).doErrorLog(exception)
        verify(sentryExceptionReporter)
            .capture(exception, HttpStatus.NOT_FOUND, ErrorCode.RESOURCE_NOT_FOUND.code)
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

    @Test
    fun `5xx BaseException은 Sentry에 전송한다`() {
        val exception = BaseException(ErrorCode.PROFILE_IMAGE_GENERATION_FAILED, "upstream failed")

        val response = handler.handleBaseException(exception)

        assertEquals(HttpStatus.BAD_GATEWAY, response.statusCode)
        assertEquals(ErrorCode.PROFILE_IMAGE_GENERATION_FAILED.code, response.body?.code)
        verify(sentryExceptionReporter).capture(
            exception,
            HttpStatus.BAD_GATEWAY,
            ErrorCode.PROFILE_IMAGE_GENERATION_FAILED.code,
        )
        verify(traceManager).doErrorLog(exception)
    }

    @Test
    fun `알 수 없는 JSON 필드는 필드명을 포함한 400 메시지를 반환한다`() {
        val exception =
            HttpMessageNotReadableException(
                "unknown field",
                UnrecognizedPropertyException(null, "unknown field", JsonLocation.NA, Any::class.java, "content", emptyList()),
                MockHttpInputMessage(byteArrayOf()),
            )

        val response = handler.handleUnreadableRequest(exception)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals(ErrorCode.MALFORMED_REQUEST_BODY.code, response.body?.code)
        assertEquals("content 필드는 이 요청에서 사용할 수 없습니다.", response.body?.errorMessage)
    }

    @Test
    fun `필수 채팅방 썸네일 파트가 없으면 구체적인 400 오류를 반환한다`() {
        val exception = mock(MissingServletRequestPartException::class.java)
        `when`(exception.requestPartName).thenReturn("thumbnail")

        val response = handler.handleMissingRequestPart(exception)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals(ErrorCode.CHAT_ROOM_THUMBNAIL_REQUIRED.code, response.body?.code)
        assertEquals(ErrorCode.CHAT_ROOM_THUMBNAIL_REQUIRED.errorMessage, response.body?.errorMessage)
        verify(sentryExceptionReporter)
            .capture(exception, HttpStatus.BAD_REQUEST, ErrorCode.CHAT_ROOM_THUMBNAIL_REQUIRED.code)
        verify(traceManager).doErrorLog(exception)
    }
}
