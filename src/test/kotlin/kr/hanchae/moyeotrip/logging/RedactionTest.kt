package kr.hanchae.moyeotrip.logging

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class RedactionTest {
    @Test
    fun `Kakao 인가 코드와 client secret은 trace body에서 마스킹한다`() {
        val redacted =
            Redaction.redactBody(
                mapOf(
                    "authorizationCode" to "one-time-code",
                    "clientSecret" to "server-secret",
                    "redirectUri" to "https://example.com/callback",
                ),
            )

        assertEquals(Redaction.REDACTED, redacted["authorizationCode"])
        assertEquals(Redaction.REDACTED, redacted["clientSecret"])
        assertFalse(redacted.values.contains("one-time-code"))
        assertFalse(redacted.values.contains("server-secret"))
    }
}
