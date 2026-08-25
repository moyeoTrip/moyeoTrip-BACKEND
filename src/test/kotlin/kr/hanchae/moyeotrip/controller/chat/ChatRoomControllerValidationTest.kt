package kr.hanchae.moyeotrip.controller.chat

import kr.hanchae.moyeotrip.service.chat.ChatRoomService
import kr.hanchae.moyeotrip.support.LoginUserIdStubResolver
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verifyNoInteractions
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class ChatRoomControllerValidationTest {
    private val chatRoomService = mock(ChatRoomService::class.java)
    private val mockMvc: MockMvc =
        MockMvcBuilders
            .standaloneSetup(ChatRoomController(chatRoomService))
            .setCustomArgumentResolvers(LoginUserIdStubResolver())
            .build()

    @Test
    fun `채팅방 생성은 빈 제목과 최대 인원 최솟값 위반을 거부한다`() {
        performCreateRoom(
            validCreateRoomJson
                .replace(
                    "\"title\":\"테스트 여행\"",
                    "\"title\":\" \"",
                ).replace("\"maxParticipants\":4", "\"maxParticipants\":2"),
        ).andExpect(status().isBadRequest)

        verifyNoInteractions(chatRoomService)
    }

    @Test
    fun `채팅방 생성은 중첩된 커스텀 코스 장소의 잘못된 ID를 거부한다`() {
        performCreateRoom(validCreateRoomJson.replaceFirst("\"contentId\":100", "\"contentId\":0"))
            .andExpect(status().isBadRequest)

        verifyNoInteractions(chatRoomService)
    }

    @Test
    fun `채팅방 생성 DTO의 문자열 숫자 나이와 중첩 코스 경계를 검증한다`() {
        val invalidRequests =
            listOf(
                validCreateRoomJson.replace("테스트 여행", "가".repeat(101)),
                validCreateRoomJson.addAfterTitle("\"description\":\"${"가".repeat(501)}\","),
                validCreateRoomJson.replace("\"maxParticipants\":4", "\"maxParticipants\":21"),
                validCreateRoomJson.replace("\"minimumParticipants\":3", "\"minimumParticipants\":2"),
                validCreateRoomJson.addAfterTitle("\"meetingLatitude\":-91,"),
                validCreateRoomJson.addAfterTitle("\"meetingLongitude\":181,"),
                validCreateRoomJson.addAfterTitle("\"meetingDetails\":\"${"가".repeat(501)}\","),
                validCreateRoomJson.addAfterTitle("\"participationFee\":-1,"),
                validCreateRoomJson.addAfterTitle("\"minimumAge\":19,"),
                validCreateRoomJson.addAfterTitle("\"maximumAge\":101,"),
                validCreateRoomJson.replace("\"title\":\"테스트 코스\"", "\"title\":\" \""),
                validCreateRoomJson.replace("\"title\":\"테스트 코스\"", "\"title\":\"${"가".repeat(101)}\""),
                validCreateRoomJson.replace(
                    "\"title\":\"테스트 코스\",",
                    "\"title\":\"테스트 코스\",\"description\":\"${"가".repeat(501)}\",",
                ),
                validCreateRoomJson.replace("\"dayNumber\":1", "\"dayNumber\":0"),
                validCreateRoomJson.replace("\"sequence\":1", "\"sequence\":0"),
            )

        invalidRequests.forEach { json ->
            performCreateRoom(json).andExpect(status().isBadRequest)
        }
        verifyNoInteractions(chatRoomService)
    }

    @Test
    fun `집합 정보 수정은 위경도 범위를 검증한다`() {
        mockMvc
            .perform(
                put("/api/v1/chat-rooms/10/meeting-info")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "meetingLatitude": 91,
                          "meetingLongitude": -181,
                          "meetingDateTime": "2026-09-12T08:30:00"
                        }
                        """.trimIndent(),
                    ),
            ).andExpect(status().isBadRequest)

        verifyNoInteractions(chatRoomService)
    }

    @Test
    fun `집합 정보 수정은 상세 안내를 500자로 제한한다`() {
        val tooLong = "가".repeat(501)
        mockMvc
            .perform(
                put("/api/v1/chat-rooms/10/meeting-info")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"meetingDetails":"$tooLong","meetingDateTime":"2026-09-12T08:30:00"}"""),
            ).andExpect(status().isBadRequest)
        verifyNoInteractions(chatRoomService)
    }

    @Test
    fun `참가 신청은 500자를 초과하는 소개를 거부한다`() {
        val tooLongMessage = "가".repeat(501)

        mockMvc
            .perform(
                post("/api/v1/chat-rooms/10/applications")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"applicationMessage":"$tooLongMessage"}"""),
            ).andExpect(status().isBadRequest)

        verifyNoInteractions(chatRoomService)
    }

    @Test
    fun `멤버 강퇴는 공백 사유를 거부한다`() {
        mockMvc
            .perform(
                delete("/api/v1/chat-rooms/10/members/2")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"reason":"   "}"""),
            ).andExpect(status().isBadRequest)

        verifyNoInteractions(chatRoomService)
    }

    @Test
    fun `멤버 강퇴 사유는 500자를 초과할 수 없다`() {
        val tooLong = "가".repeat(501)
        mockMvc
            .perform(
                delete("/api/v1/chat-rooms/10/members/2")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"reason":"$tooLong"}"""),
            ).andExpect(status().isBadRequest)
        verifyNoInteractions(chatRoomService)
    }

    @Test
    fun `공지 등록은 공백 내용을 거부한다`() {
        mockMvc
            .perform(
                post("/api/v1/chat-rooms/10/notices")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"notice":" ","pinned":true}"""),
            ).andExpect(status().isBadRequest)

        verifyNoInteractions(chatRoomService)
    }

    @Test
    fun `공지 등록과 수정은 내용을 1000자로 제한한다`() {
        val tooLong = "가".repeat(1001)
        mockMvc
            .perform(
                post("/api/v1/chat-rooms/10/notices")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"notice":"$tooLong"}"""),
            ).andExpect(status().isBadRequest)
        mockMvc
            .perform(
                put("/api/v1/chat-rooms/10/notices/20")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"notice":"$tooLong"}"""),
            ).andExpect(status().isBadRequest)
        verifyNoInteractions(chatRoomService)
    }

    @Test
    fun `일반 메시지는 1000자를 초과하면 거부한다`() {
        val tooLongMessage = "가".repeat(1001)

        mockMvc
            .perform(
                post("/api/v1/chat-rooms/10/messages")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"content":"$tooLongMessage"}"""),
            ).andExpect(status().isBadRequest)

        verifyNoInteractions(chatRoomService)
    }

    @Test
    fun `일반 메시지는 공백일 수 없다`() {
        mockMvc
            .perform(
                post("/api/v1/chat-rooms/10/messages")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"content":" "}"""),
            ).andExpect(status().isBadRequest)
        verifyNoInteractions(chatRoomService)
    }

    @Test
    fun `투표 생성은 두 개 미만의 선택지를 거부한다`() {
        mockMvc
            .perform(
                post("/api/v1/chat-rooms/10/messages/polls")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"question":"점심 메뉴","options":["한식"]}"""),
            ).andExpect(status().isBadRequest)

        verifyNoInteractions(chatRoomService)
    }

    @Test
    fun `투표 질문과 각 선택지의 공백 및 길이를 검증한다`() {
        val longQuestion = "가".repeat(201)
        val longOption = "가".repeat(101)
        val invalidRequests =
            listOf(
                """{"question":" ","options":["한식","양식"]}""",
                """{"question":"$longQuestion","options":["한식","양식"]}""",
                """{"question":"메뉴","options":[" ","양식"]}""",
                """{"question":"메뉴","options":["$longOption","양식"]}""",
                """{"question":"메뉴","options":["1","2","3","4","5","6"]}""",
            )
        invalidRequests.forEach { json ->
            mockMvc
                .perform(
                    post("/api/v1/chat-rooms/10/messages/polls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json),
                ).andExpect(status().isBadRequest)
        }
        verifyNoInteractions(chatRoomService)
    }

    @Test
    fun `정산 메모는 공백이거나 1000자를 초과할 수 없다`() {
        listOf(" ", "가".repeat(1001)).forEach { memo ->
            mockMvc
                .perform(
                    post("/api/v1/chat-rooms/10/messages/settlement-memos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"memo":"$memo"}"""),
                ).andExpect(status().isBadRequest)
        }
        verifyNoInteractions(chatRoomService)
    }

    private fun performCreateRoom(json: String) =
        mockMvc.perform(
            multipart("/api/v1/chat-rooms")
                .file(
                    MockMultipartFile(
                        "request",
                        "request.json",
                        MediaType.APPLICATION_JSON_VALUE,
                        json.toByteArray(),
                    ),
                ),
        )

    private fun String.addAfterTitle(value: String): String = replace("\"title\":\"테스트 여행\",", "\"title\":\"테스트 여행\",$value")

    companion object {
        private val validCreateRoomJson =
            """
            {
              "title":"테스트 여행",
              "minimumParticipants":3,
              "maxParticipants":4,
              "tripType":"DAY_TRIP",
              "startDate":"2026-09-12",
              "recruitmentDeadlineDate":"2026-09-09",
              "dayTripStartTime":"09:00",
              "dayTripEndTime":"18:00",
              "meetingDateTime":"2026-09-12T08:30:00",
              "genderRestriction":"NONE",
              "joinApprovalMode":"MANUAL",
              "courseType":"CUSTOM",
              "customCourse":{
                "title":"테스트 코스",
                "places":[
                  {"contentId":100,"dayNumber":1,"sequence":1,"visitTime":"09:00"},
                  {"contentId":101,"dayNumber":1,"sequence":2,"visitTime":"11:00"}
                ]
              }
            }
            """.trimIndent()
    }
}
