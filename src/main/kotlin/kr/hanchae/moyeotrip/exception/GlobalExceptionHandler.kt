package kr.hanchae.moyeotrip.exception

import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException
import kr.hanchae.moyeotrip.logging.SentryExceptionReporter
import kr.hanchae.moyeotrip.logging.TraceManager
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.resource.NoResourceFoundException

@RestControllerAdvice
class GlobalExceptionHandler(
    private val traceManager: TraceManager,
    private val sentryExceptionReporter: SentryExceptionReporter,
) {
    @ExceptionHandler(BaseException::class)
    fun handleBaseException(exception: BaseException): ResponseEntity<ErrorResponse> {
        sentryExceptionReporter.capture(exception, exception.errorCode.httpStatus, exception.errorCode.code)
        return ResponseEntity
            .status(exception.errorCode.httpStatus)
            .contentType(MediaType.APPLICATION_JSON)
            .body(ErrorResponse.of(exception.errorCode, exception.message))
            .apply {
                traceManager.doErrorLog(exception)
            }
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(exception: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        sentryExceptionReporter.capture(exception, HttpStatus.BAD_REQUEST, ErrorCode.BAD_REQUEST.code)
        val message =
            exception.bindingResult.fieldErrors
                .firstOrNull()
                ?.defaultMessage
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .contentType(MediaType.APPLICATION_JSON)
            .body(ErrorResponse.of(ErrorCode.BAD_REQUEST, message))
            .apply {
                traceManager.doErrorLog(exception)
            }
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleUnreadableRequest(exception: HttpMessageNotReadableException): ResponseEntity<ErrorResponse> {
        sentryExceptionReporter.capture(exception, HttpStatus.BAD_REQUEST, ErrorCode.MALFORMED_REQUEST_BODY.code)
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .contentType(MediaType.APPLICATION_JSON)
            .body(ErrorResponse.of(ErrorCode.MALFORMED_REQUEST_BODY, unreadableRequestMessage(exception)))
            .apply {
                traceManager.doErrorLog(exception)
            }
    }

    private fun unreadableRequestMessage(exception: HttpMessageNotReadableException): String {
        val unknownProperty =
            generateSequence(exception as Throwable?) { it.cause }
                .filterIsInstance<UnrecognizedPropertyException>()
                .firstOrNull()
                ?.propertyName
        if (unknownProperty != null) {
            return "$unknownProperty 필드는 이 요청에서 사용할 수 없습니다."
        }
        val fieldName =
            generateSequence(exception as Throwable?) { it.cause }
                .filterIsInstance<JsonMappingException>()
                .flatMap { it.path.asSequence() }
                .mapNotNull { it.fieldName }
                .lastOrNull()
        return fieldName?.let { "$it 필드의 형식 또는 필수 여부를 확인해 주세요." }
            ?: ErrorCode.MALFORMED_REQUEST_BODY.errorMessage
    }

    @ExceptionHandler(NoResourceFoundException::class)
    fun handleNoResourceFound(exception: NoResourceFoundException): ResponseEntity<ErrorResponse> {
        sentryExceptionReporter.capture(exception, HttpStatus.NOT_FOUND, ErrorCode.RESOURCE_NOT_FOUND.code)
        return ResponseEntity
            .status(ErrorCode.RESOURCE_NOT_FOUND.httpStatus)
            .contentType(MediaType.APPLICATION_JSON)
            .body(ErrorResponse.of(ErrorCode.RESOURCE_NOT_FOUND, ErrorCode.RESOURCE_NOT_FOUND.errorMessage))
            .apply {
                traceManager.doErrorLog(exception)
            }
    }

    @ExceptionHandler(Exception::class)
    fun handleUnexpectedException(exception: Exception): ResponseEntity<ErrorResponse> {
        sentryExceptionReporter.capture(exception, HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_SERVER_ERROR.code)
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .contentType(MediaType.APPLICATION_JSON)
            .body(ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_SERVER_ERROR.errorMessage))
            .apply {
                traceManager.doErrorLog(exception)
            }
    }
}
