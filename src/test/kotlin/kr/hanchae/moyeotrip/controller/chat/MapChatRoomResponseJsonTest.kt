package kr.hanchae.moyeotrip.controller.chat

import com.fasterxml.jackson.databind.ObjectMapper
import kr.hanchae.moyeotrip.controller.chat.response.MapChatRoomResponse
import kr.hanchae.moyeotrip.controller.tour.response.TravelCourseTagResponse
import kr.hanchae.moyeotrip.entity.chat.ChatRoomStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MapChatRoomResponseJsonTest {
    private val objectMapper = ObjectMapper()

    @Test
    fun `지도 채팅방 태그 필드명은 기존 응답과 동일하게 name이다`() {
        val response =
            MapChatRoomResponse(
                roomId = 101L,
                title = "주왕산 트레킹",
                thumbnail = null,
                status = ChatRoomStatus.RECRUITING,
                favorite = false,
                participantCount = 2,
                maxParticipants = 5,
                tags = listOf(TravelCourseTagResponse(tagId = 1L, name = "자연")),
                meetingLatitude = 36.576,
                meetingLongitude = 128.97,
                meetingDetails = "안동역",
                distanceMeters = 842L,
            )

        val tag = objectMapper.valueToTree<com.fasterxml.jackson.databind.JsonNode>(response).path("tags").first()

        assertTrue(tag.has("name"))
        assertEquals("자연", tag.path("name").asText())
        assertFalse(tag.has("tagName"))
    }
}
