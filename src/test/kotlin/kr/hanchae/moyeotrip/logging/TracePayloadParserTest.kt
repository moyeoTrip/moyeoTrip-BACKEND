package kr.hanchae.moyeotrip.logging

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TracePayloadParserTest {
    @Test
    fun `multipart binary 빈 본문과 큰 본문은 파싱하지 않는다`() {
        assertEquals(
            mapOf("_omitted" to Redaction.OMITTED_MULTIPART),
            TracePayloadParser.buildBody("body", "multipart/form-data; boundary=test"),
        )
        assertEquals(
            mapOf("_omitted" to Redaction.OMITTED_BINARY),
            TracePayloadParser.buildBody("body", "image/png"),
        )
        assertEquals(emptyMap<String, Any?>(), TracePayloadParser.buildBody(null, "application/json"))
        assertEquals(emptyMap<String, Any?>(), TracePayloadParser.buildBody("   ", null))

        val large = "a".repeat(TraceManager.MAX_RAW_BODY_LENGTH + 1)
        assertEquals(
            mapOf("_omitted" to Redaction.OMITTED_TOO_LARGE, "size" to large.length),
            TracePayloadParser.buildBody(large, "application/json"),
        )
    }

    @Test
    fun `JSON 객체 목록과 단일 값을 구조를 유지해 변환한다`() {
        val objectBody = TracePayloadParser.buildBody("""{"name":"여행","accessToken":"secret"}""", "application/json")
        val listBody = TracePayloadParser.buildBody("""[{"name":"경주"},{"password":"secret"}]""", "application/json")
        val scalarBody = TracePayloadParser.buildBody("123", "application/json")
        val nullBody = TracePayloadParser.buildBody("null", "application/json")

        assertEquals("여행", objectBody["name"])
        assertEquals(Redaction.REDACTED, objectBody["accessToken"])
        assertTrue(listBody["_list"] is List<*>)
        assertEquals(123, scalarBody["original"])
        assertTrue(nullBody.containsKey("original"))
        assertNull(nullBody["original"])
    }

    @Test
    fun `JSON이 아닌 원문에서도 자격 증명을 마스킹한다`() {
        val result = TracePayloadParser.buildBody("password=my-secret&name=test", "text/plain")

        val original = result["original"].toString()
        assertTrue(original.contains(Redaction.REDACTED))
        assertTrue(!original.contains("my-secret"))
    }

    @Test
    fun `헤더는 대소문자와 빈 값에 안전하게 조회한다`() {
        val headers = mapOf("CONTENT-TYPE" to listOf("application/json"), "Empty" to emptyList())

        assertEquals("application/json", TracePayloadParser.findHeader(headers, "content-type"))
        assertNull(TracePayloadParser.findHeader(headers, "missing"))
        assertNull(TracePayloadParser.findHeader(headers, "empty"))
    }

    @Test
    fun `쿼리 문자열을 디코딩하고 중복 키의 마지막 값과 값 없는 플래그를 유지한다`() {
        assertEquals(emptyMap<String, Any>(), TracePayloadParser.queryToMap(null))
        assertEquals(emptyMap<String, Any>(), TracePayloadParser.queryToMap("  "))

        val result = TracePayloadParser.queryToMap("keyword=%EA%B2%BD%EC%A3%BC&flag&keyword=%EC%95%88%EB%8F%99&&bad=%ZZ")

        assertEquals("안동", result["keyword"])
        assertEquals("", result["flag"])
        assertEquals("%ZZ", result["bad"])
    }
}
