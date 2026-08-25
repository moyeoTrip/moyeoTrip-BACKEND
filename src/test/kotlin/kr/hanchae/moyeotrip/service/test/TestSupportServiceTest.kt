package kr.hanchae.moyeotrip.service.test

import kr.hanchae.moyeotrip.entity.chat.ChatRoom
import kr.hanchae.moyeotrip.entity.chat.ChatRoomStatus
import kr.hanchae.moyeotrip.entity.tour.TravelCourse
import kr.hanchae.moyeotrip.entity.tour.TravelCourseType
import kr.hanchae.moyeotrip.entity.user.User
import kr.hanchae.moyeotrip.entity.user.UserRole
import kr.hanchae.moyeotrip.repository.ChatRoomRepository
import kr.hanchae.moyeotrip.repository.UserRepository
import kr.hanchae.moyeotrip.utils.jwt.JwtUtil
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.time.LocalDate
import java.time.LocalTime
import java.util.Optional

class TestSupportServiceTest {
    private val userRepository = mock(UserRepository::class.java)
    private val chatRoomRepository = mock(ChatRoomRepository::class.java)
    private val jwtUtil = mock(JwtUtil::class.java)
    private val service = TestSupportService(userRepository, chatRoomRepository, jwtUtil)

    @Test
    fun `당일 여행을 QA용 완료 여행으로 전환한다`() {
        val room = room()
        val completedDate = LocalDate.now().minusDays(1)
        `when`(chatRoomRepository.findById(10L)).thenReturn(Optional.of(room))

        val response = service.completeChatRoom(10L)

        assertEquals(10L, response.roomId)
        assertEquals(ChatRoomStatus.CONFIRMED, response.status)
        assertEquals(completedDate, response.startDate)
        assertEquals(completedDate.minusDays(1), response.recruitmentDeadlineDate)
        assertNull(response.endDate)
        assertTrue(response.completed)
        verify(chatRoomRepository).completeForTest(10L, completedDate, completedDate.minusDays(1), null)
    }

    @Test
    fun `숙박 여행도 QA용 완료 처리 시 시작일 마감일 종료일을 모두 과거로 전환한다`() {
        val room = room(endDate = LocalDate.now().plusDays(4))
        val completedEndDate = LocalDate.now().minusDays(1)
        `when`(chatRoomRepository.findById(10L)).thenReturn(Optional.of(room))

        val response = service.completeChatRoom(10L)

        assertEquals(completedEndDate.minusDays(1), response.startDate)
        assertEquals(completedEndDate.minusDays(2), response.recruitmentDeadlineDate)
        assertEquals(completedEndDate, response.endDate)
        assertTrue(response.completed)
        verify(chatRoomRepository).completeForTest(
            10L,
            completedEndDate.minusDays(1),
            completedEndDate.minusDays(2),
            completedEndDate,
        )
    }

    private fun room(endDate: LocalDate? = null): ChatRoom {
        val startDate = LocalDate.now().plusDays(3)
        return ChatRoom(
            id = 10L,
            host = User(id = 1L, userRole = UserRole.ROLE_USER),
            course = TravelCourse(id = 1L, type = TravelCourseType.PUBLIC, title = "테스트 코스"),
            roomTitle = "테스트 채팅방",
            maxParticipants = 3,
            startDate = startDate,
            endDate = endDate,
            recruitmentDeadlineDate = startDate,
            dayTripStartTime = if (endDate == null) LocalTime.of(9, 0) else null,
            dayTripEndTime = if (endDate == null) LocalTime.of(18, 0) else null,
            meetingDateTime = startDate.atTime(8, 30),
        )
    }
}
