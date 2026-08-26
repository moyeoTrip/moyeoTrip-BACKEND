package kr.hanchae.moyeotrip.config

import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException
import kr.hanchae.moyeotrip.controller.chat.request.CreateChatRoomNoticeRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class JacksonConfigTest {
    private val objectMapper = JacksonConfig().objectMapper()

    @Test
    fun `요청 DTO에 정의되지 않은 JSON 필드는 역직렬화를 거부한다`() {
        val exception =
            assertThrows(UnrecognizedPropertyException::class.java) {
                objectMapper.readValue(
                    """{"notice":"준비물 공지","pinned":false,"content":"정의되지 않은 필드"}""",
                    CreateChatRoomNoticeRequest::class.java,
                )
            }

        assertEquals("content", exception.propertyName)
    }
}
