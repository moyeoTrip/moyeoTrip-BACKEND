package kr.hanchae.moyeotrip.controller.test

import io.swagger.v3.oas.annotations.Operation
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TestSupportAPISpecTest {
    @Test
    fun `테스트 토큰 발급 API는 Swagger에서 숨긴다`() {
        val operation =
            TestSupportAPISpec::class.java
                .getDeclaredMethod("issueAccessToken", Long::class.javaPrimitiveType)
                .getAnnotation(Operation::class.java)

        assertTrue(operation.hidden)
    }
}
