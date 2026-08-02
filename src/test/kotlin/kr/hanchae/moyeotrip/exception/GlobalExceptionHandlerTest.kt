package kr.hanchae.moyeotrip.exception

import kr.hanchae.moyeotrip.logging.TraceManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.servlet.resource.NoResourceFoundException

class GlobalExceptionHandlerTest {
    private val traceManager = mock(TraceManager::class.java)
    private val handler = GlobalExceptionHandler(traceManager)

    @Test
    fun `존재하지 않는 API 경로는 404를 반환한다`() {
        val exception = mock(NoResourceFoundException::class.java)

        val response = handler.handleNoResourceFound(exception)

        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
        assertEquals(MediaType.APPLICATION_JSON, response.headers.contentType)
        assertEquals(ErrorCode.RESOURCE_NOT_FOUND.code, response.body?.code)
        assertEquals(ErrorCode.RESOURCE_NOT_FOUND.errorMessage, response.body?.errorMessage)
        verify(traceManager).doErrorLog(exception)
    }
}
