package kr.hanchae.moyeotrip.controller.chat

import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponses
import kr.hanchae.moyeotrip.config.JacksonConfig
import kr.hanchae.moyeotrip.controller.chat.request.CreateChatRoomRequest
import kr.hanchae.moyeotrip.entity.chat.JoinApprovalMode
import kr.hanchae.moyeotrip.entity.chat.TripType
import kr.hanchae.moyeotrip.entity.user.Gender
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ChatRoomRequestSwaggerTest {
    @Test
    fun `enum 스키마는 값 목록을 중복 선언하지 않는다`() {
        assertEquals(
            emptyList<String>(),
            TripType::class.java
                .getAnnotation(Schema::class.java)
                .allowableValues
                .toList(),
        )
        assertEquals(
            emptyList<String>(),
            Gender::class.java
                .getAnnotation(Schema::class.java)
                .allowableValues
                .toList(),
        )
    }

    @Test
    fun `채팅방 생성 요청의 승인 방식 Swagger 예시는 지원하는 enum 값이다`() {
        val schema = CreateChatRoomRequest::class.java.getDeclaredField("joinApprovalMode").getAnnotation(Schema::class.java)

        assertEquals(JoinApprovalMode.MANUAL.name, schema.example)
    }

    @Test
    fun `채팅방 생성의 400 응답은 모든 도메인 검증 오류 예시를 제공한다`() {
        val method = ChatRoomAPISpec::class.java.declaredMethods.single { it.name == "createRoom" }
        val responses = method.getAnnotation(ApiResponses::class.java)
        val badRequest = responses.value.single { it.responseCode == "400" }

        assertEquals(
            setOf("요청 본문 또는 enum 값 오류", "당일·숙박 일정 입력 오류", "참가 나이 범위 오류", "커스텀 코스 일차·순서 구성 오류"),
            badRequest.content
                .single()
                .examples
                .map { it.name }
                .toSet(),
        )
    }

    @Test
    fun `채팅방 생성 Swagger 예시는 유효한 1박 2일 커스텀 코스 요청이다`() {
        val example = CreateChatRoomRequest::class.java.getAnnotation(Schema::class.java).example
        val request = JacksonConfig().objectMapper().readValue(example, CreateChatRoomRequest::class.java)

        assertEquals("OVERNIGHT", request.tripType.name)
        assertEquals(null, request.dayTripStartTime)
        assertEquals(null, request.dayTripEndTime)
        assertEquals(null, request.courseId)
        assertEquals(
            setOf(1, 2),
            request.customCourse
                ?.places
                ?.map { it.dayNumber }
                ?.toSet(),
        )
    }
}
