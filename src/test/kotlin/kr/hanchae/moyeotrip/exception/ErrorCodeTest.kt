package kr.hanchae.moyeotrip.exception

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ErrorCodeTest {
    @Test
    fun `오류 코드 숫자는 서로 중복되지 않는다`() {
        val duplicatedCodes =
            ErrorCode.entries
                .groupBy(ErrorCode::code)
                .filterValues { it.size > 1 }

        assertEquals(emptyMap<Int, List<ErrorCode>>(), duplicatedCodes)
    }
}
