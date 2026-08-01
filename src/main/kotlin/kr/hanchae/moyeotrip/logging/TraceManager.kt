package kr.hanchae.moyeotrip.logging

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import kr.hanchae.moyeotrip.utils.MoyeoTripJsonMappers
import org.springframework.boot.actuate.web.exchanges.HttpExchange
import org.springframework.stereotype.Component
import org.springframework.web.context.annotation.RequestScope
import org.springframework.web.util.ContentCachingRequestWrapper
import org.springframework.web.util.ContentCachingResponseWrapper
import tools.jackson.databind.PropertyNamingStrategies
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule
import tools.jackson.module.kotlin.readValue
import java.io.PrintWriter
import java.io.StringWriter
import java.net.URLDecoder
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.Collections

@Component
@RequestScope
class TraceManager {
    var throwable: Throwable? = null
    private val checkpoints = mutableListOf<String>()

    var httpTrace: HttpExchange? = null
    var wrappedRequest: ContentCachingRequestWrapper? = null
    var wrappedResponse: ContentCachingResponseWrapper? = null

    fun doErrorLog(throwable: Throwable) {
        this.throwable = throwable
    }

    fun isErrorLog() = throwable != null

    /**
     * Trace 생성 — 민감 정보는 [Redaction] 으로 마스킹.
     * httpTrace 가 아직 set 안 됐으면 null 반환 (방어).
     */
    fun getTrace(): Trace? {
        val trace = httpTrace ?: return null
        val req = wrappedRequest
        val res = wrappedResponse

        // HTTP 헤더 이름은 case-insensitive — `Content-Type` / `content-type` / `CONTENT-TYPE` 모두 매치.
        // 또한 wrappedRequest/Response 의 contentType 을 우선 사용 (Servlet API 가 항상 정상 case-insensitive 처리).
        val requestContentType = req?.contentType ?: TracePayloadParser.findHeader(trace.request.headers, "Content-Type")
        val responseContentType = res?.contentType ?: TracePayloadParser.findHeader(trace.response.headers, "Content-Type")

        val request =
            TraceRequest(
                headers = Redaction.redactHeaders(trace.request.headers),
                params =
                    Redaction.redactQueryParams(
                        TracePayloadParser.queryToMap(
                            trace.request.uri.query,
                        ),
                    ),
                body =
                    TracePayloadParser.buildBody(
                        req?.contentAsByteArray?.toString(Charset.defaultCharset()),
                        requestContentType,
                    ),
            )

        val response =
            TraceResponse(
                status = trace.response.status,
                headers = Redaction.redactHeaders(trace.response.headers),
                body =
                    TracePayloadParser.buildBody(
                        res?.contentAsByteArray?.toString(Charset.defaultCharset()),
                        responseContentType,
                    ),
            )

        return Trace(
            timestamp = trace.timestamp.toString(),
            traceRequest = request,
            traceResponse = response,
            elapsed = trace.timeTaken?.toMillis() ?: 0L,
            path = trace.request.uri.path,
            method = trace.request.method,
            remoteAddress = req?.remoteAddr ?: "unknown",
            cause = throwableToStackTrace(throwable),
            errorType = throwable?.javaClass,
            checkpoints = if (checkpoints.isEmpty()) null else checkpoints,
            logType = if (isErrorLog()) LogType.ERROR else LogType.PROTOCOL,
        )
    }

    /** Security에서 필터 체인을 종료해 Actuator HttpExchange가 생성되지 않은 경우의 trace. */
    fun getFilterTrace(
        request: HttpServletRequest,
        response: HttpServletResponse,
        startedAt: Instant,
        elapsedMillis: Long,
    ): Trace {
        val req = wrappedRequest
        val res = wrappedResponse
        val requestHeaders =
            Collections.list(request.headerNames).associateWith { name ->
                Collections.list(request.getHeaders(name))
            }
        val responseHeaders = response.headerNames.associateWith { name -> response.getHeaders(name).toList() }

        return Trace(
            timestamp = startedAt.toString(),
            traceRequest =
                TraceRequest(
                    headers = Redaction.redactHeaders(requestHeaders),
                    params = Redaction.redactQueryParams(TracePayloadParser.queryToMap(request.queryString)),
                    body =
                        TracePayloadParser.buildBody(
                            req?.contentAsByteArray?.toString(Charset.defaultCharset()),
                            req?.contentType ?: request.contentType,
                        ),
                ),
            traceResponse =
                TraceResponse(
                    status = response.status,
                    headers = Redaction.redactHeaders(responseHeaders),
                    body =
                        TracePayloadParser.buildBody(
                            res?.contentAsByteArray?.toString(Charset.defaultCharset()),
                            res?.contentType ?: response.contentType,
                        ),
                ),
            elapsed = elapsedMillis,
            path = request.requestURI,
            method = request.method,
            remoteAddress = request.remoteAddr ?: "unknown",
            cause = throwableToStackTrace(throwable),
            errorType = throwable?.javaClass,
            checkpoints = if (checkpoints.isEmpty()) null else checkpoints,
            logType = if (isErrorLog()) LogType.ERROR else LogType.PROTOCOL,
        )
    }

    companion object {
        /** 본문 캡처가 너무 커지지 않도록 안전 cap. */
        const val MAX_RAW_BODY_LENGTH = 8 * 1024

        fun throwableToStackTrace(throwable: Throwable?): String? {
            if (throwable == null) return null

            val stackTrace = StringWriter()
            throwable.printStackTrace(PrintWriter(stackTrace))
            stackTrace.flush()

            return stackTrace.toString()
        }
    }
}

internal object TracePayloadParser {
    val default: JsonMapper =
        JsonMapper
            .builder()
            .addModule(KotlinModule.Builder().build())
            .propertyNamingStrategy(PropertyNamingStrategies.LOWER_CAMEL_CASE)
            .build()

    /**
     * 본문을 안전하게 캡처.
     * - multipart 면 OMITTED_MULTIPART (이미지 업로드 등이 로그 폭주시키지 않게)
     * - binary 면 OMITTED_BINARY
     * - raw size 가 [TraceManager.MAX_RAW_BODY_LENGTH] 초과면 **본문은 통째로 OMITTED_TOO_LARGE + size 만 기록**.
     *   (캐시 자체는 이미 메모리에 누적된 상태 — 진짜 OOM 방어는 [TraceFilter] 의 `captureResponseBody` opt-out.
     *   본 단계는 trace 직렬화·전송량과 클라이언트 노출 한도.)
     *   클라이언트 디버깅 시 본문이 안 보이면 size 값으로 "정상 응답이지만 너무 커서 묻혔다" 라고 식별 가능.
     * - JSON 파싱 성공: 민감 키 redact
     * - 파싱 실패: 원본 문자열 — credential 패턴은 regex 로 mask
     */
    fun buildBody(
        raw: String?,
        contentType: String?,
    ): Map<String, Any?> {
        if (Redaction.isMultipart(contentType)) return mapOf("_omitted" to Redaction.OMITTED_MULTIPART)
        if (Redaction.isBinary(contentType)) return mapOf("_omitted" to Redaction.OMITTED_BINARY)
        if (raw.isNullOrBlank()) return emptyMap()
        if (raw.length > TraceManager.MAX_RAW_BODY_LENGTH) {
            return mapOf("_omitted" to Redaction.OMITTED_TOO_LARGE, "size" to raw.length)
        }

        return try {
            // top-level Map / List 모두 안전하게 처리 — List 면 `_list` 키로 감싸 응답 구조 유지.
            when (val parsed = MoyeoTripJsonMappers.default.readValue<Any?>(raw)) {
                is Map<*, *> -> {
                    @Suppress("UNCHECKED_CAST")
                    Redaction.redactBody(parsed as Map<String, Any?>)
                }

                is List<*> -> {
                    mapOf("_list" to Redaction.redactList(parsed))
                }

                else -> {
                    mapOf("original" to parsed)
                }
            }
        } catch (_: Exception) {
            // JSON 파싱 실패 — malformed JSON / form-urlencoded / 평문. raw 라도 credential 패턴은 mask.
            mapOf("original" to Redaction.redactRaw(raw).take(TraceManager.MAX_RAW_BODY_LENGTH))
        }
    }

    /** HTTP 헤더 case-insensitive lookup — first occurrence 의 first value 반환. */
    fun findHeader(
        headers: Map<String, List<String>>,
        name: String,
    ): String? =
        headers.entries
            .firstOrNull { it.key.equals(name, ignoreCase = true) }
            ?.value
            ?.firstOrNull()

    /**
     * 쿼리스트링 → 맵. 안전한 파서:
     * - 동일 key 중복 시 마지막 값 유지 (관행)
     * - `?flag` 처럼 값 없는 키는 ""로
     * - URL-decode 적용
     */
    fun queryToMap(query: String?): Map<String, Any> {
        if (query.isNullOrBlank()) return emptyMap()
        return query
            .split("&")
            .filter { it.isNotBlank() }
            .associate { pair ->
                val eq = pair.indexOf('=')
                val key =
                    if (eq < 0) pair else pair.substring(0, eq)
                val value =
                    if (eq < 0) "" else pair.substring(eq + 1)
                urlDecode(key) to urlDecode(value)
            }
    }

    private fun urlDecode(s: String): String = runCatching { URLDecoder.decode(s, StandardCharsets.UTF_8) }.getOrDefault(s)
}
