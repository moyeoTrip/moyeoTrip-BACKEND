package kr.hanchae.moyeotrip.logging

/**
 * 요청/응답 trace 에서 마스킹할 민감 정보 패턴.
 *
 * - 헤더는 모두 lowercase 로 비교 (HTTP 헤더 case-insensitive).
 * - 본문 키도 lowercase 로 비교 (`accessToken` / `access_token` 등 표기 차이 흡수).
 * - multipart / binary body 는 통째로 생략 (재사용 가치 없고 로그 폭주 위험).
 */
object Redaction {
    const val REDACTED = "***REDACTED***"
    const val OMITTED_MULTIPART = "***MULTIPART_OMITTED***"
    const val OMITTED_BINARY = "***BINARY_OMITTED***"
    const val OMITTED_TOO_LARGE = "***TOO_LARGE_OMITTED***"

    /** Bearer UUID / 카카오 토큰 / 쿠키 등 — 노출 시 즉시 credential 재사용 가능. */
    private val SENSITIVE_HEADERS =
        setOf(
            "authorization",
            "cookie",
            "set-cookie",
            "proxy-authorization",
            "x-api-key",
            "x-auth-token",
        )

    /**
     * 본문 JSON 의 민감 필드 — exact match.
     * 추가로 [SENSITIVE_BODY_KEY_SUFFIXES] 의 패턴이 키 끝에 붙으면(예: `pushToken`, `kakaoAccessToken`) 자동 마스킹.
     */
    private val SENSITIVE_BODY_KEYS =
        setOf(
            "password",
            "secret",
            "token",
            "apikey",
            "api_key",
            "authorization",
            "authorizationcode",
            "authorization_code",
        )

    /** 키가 이 패턴 중 하나로 끝나면 마스킹 — `pushToken`, `kakaoAccessToken` 등. */
    private val SENSITIVE_BODY_KEY_SUFFIXES =
        listOf(
            "token",
            "_token",
            "secret",
            "_secret",
            "password",
            "_password",
            "apikey",
            "api_key",
        )

    /**
     * JSON 파싱 실패 시 raw body 에 그대로 남는 credential 을 줄이기 위한 fallback 정규식.
     * `"accessToken":"…"` / `accessToken=…` 류를 키 단어 기반으로 mask.
     */
    /**
     * 키 매칭에 사용되는 정규식 알파벳 — IGNORE_CASE 로 검사하므로 단일 case 로 충분.
     * camelCase / snake_case / kebab-case / ALL_CAPS 모두 동일 패턴이 잡아낸다.
     */
    private const val SENSITIVE_KEY_PATTERN =
        "(?:token|_token|secret|_secret|password|_password|" +
            "kakao[_-]?id|api[_-]?key|authorization[_-]?code|invite[_-]?code|numeric[_-]?code)"

    /**
     * 키 prefix — camelCase / snake_case / kebab-case 모두 받기 위해 `[A-Za-z0-9_-]*` 그대로.
     * 단어 중간에 `token` 같은 substring 이 박혀도 매치되는 false-positive 위험은 있으나, 정상 응답 필드
     * 이름에 그런 substring 이 우연히 들어갈 가능성이 낮고 false-negative(credential leak) 가 더 심각해
     * 의도적으로 prefix 를 느슨하게 둔다.
     */
    private val SENSITIVE_RAW_PATTERNS =
        listOf(
            // JSON value (string): "xxxToken":"value"
            Regex("""("[A-Za-z0-9_-]*$SENSITIVE_KEY_PATTERN"\s*:\s*)"[^"]*"""", RegexOption.IGNORE_CASE),
            // JSON value (primitive — number / boolean / null): "xxxToken":428193 / true / null
            // value 끝은 `,`, `}`, `]`, whitespace 직전까지 — quote 가 없으므로 다음 구분자에서 멈춤.
            Regex("""("[A-Za-z0-9_-]*$SENSITIVE_KEY_PATTERN"\s*:\s*)([^,}\]\s"][^,}\]]*)""", RegexOption.IGNORE_CASE),
            // form-urlencoded / query-style: xxxToken=value (hyphenated 키도 동일 패턴)
            Regex("""([A-Za-z0-9_-]*$SENSITIVE_KEY_PATTERN)=([^&\s"]+)""", RegexOption.IGNORE_CASE),
        )

    /** body 캡처를 생략할 Content-Type prefix. */
    private val BINARY_CONTENT_TYPE_PREFIXES =
        listOf(
            "image/",
            "audio/",
            "video/",
            "application/octet-stream",
            "application/pdf",
            "application/zip",
        )

    fun redactHeaders(headers: Map<String, List<String>>): Map<String, List<String>> =
        headers.mapValues { (key, values) ->
            if (key.lowercase() in SENSITIVE_HEADERS) listOf(REDACTED) else values
        }

    fun redactBody(body: Map<String, Any?>): Map<String, Any?> =
        body.mapValues { (key, value) ->
            when {
                isSensitiveBodyKey(key) -> REDACTED
                else -> redactValue(value)
            }
        }

    /** Top-level List (JSON array) 가 들어왔을 때 사용 — 원소가 Map 이면 재귀 redact, 아니면 그대로. */
    fun redactList(list: List<*>): List<Any?> = list.map { redactValue(it) }

    /**
     * Query parameter map 도 body 와 동일한 키 정책으로 마스킹.
     * `?accessToken=...` / `?authorization_code=...` / `?api_key=...` 등 민감 query 값을
     * trace 로그에 평문으로 남기지 않기 위함.
     */
    fun redactQueryParams(params: Map<String, Any>): Map<String, Any> =
        params.mapValues { (key, value) ->
            if (isSensitiveBodyKey(key)) REDACTED else value
        }

    /** raw body (JSON 파싱 실패·form-urlencoded·malformed) 에서 credential 패턴을 마스킹. */
    fun redactRaw(raw: String): String =
        SENSITIVE_RAW_PATTERNS.fold(raw) { acc, pattern ->
            when (pattern.pattern.startsWith("(\"")) {
                true -> pattern.replace(acc) { match -> "${match.groupValues[1]}\"$REDACTED\"" }
                false -> pattern.replace(acc) { match -> "${match.groupValues[1]}=$REDACTED" }
            }
        }

    /** 키 정규화 — case 무시 + hyphen 을 underscore 와 동일 취급. */
    private fun normalizeKey(key: String): String = key.lowercase().replace('-', '_')

    private fun isSensitiveBodyKey(key: String): Boolean {
        val k = normalizeKey(key)
        if (k in SENSITIVE_BODY_KEYS) return true
        return SENSITIVE_BODY_KEY_SUFFIXES.any { k.endsWith(it) }
    }

    private fun redactValue(value: Any?): Any? =
        when (value) {
            is Map<*, *> -> {
                @Suppress("UNCHECKED_CAST")
                redactBody(value as Map<String, Any?>)
            }

            is List<*> -> {
                value.map { redactValue(it) }
            }

            else -> {
                value
            }
        }

    /** Content-Type 이 multipart 인지. */
    fun isMultipart(contentType: String?): Boolean = contentType?.lowercase()?.startsWith("multipart/") == true

    /** Content-Type 이 binary 인지 (image, pdf, zip 등). */
    fun isBinary(contentType: String?): Boolean {
        val ct = contentType?.lowercase() ?: return false
        return BINARY_CONTENT_TYPE_PREFIXES.any { ct.startsWith(it) }
    }
}
