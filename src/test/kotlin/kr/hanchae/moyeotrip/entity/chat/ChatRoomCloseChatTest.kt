package kr.hanchae.moyeotrip.entity.chat

import kr.hanchae.moyeotrip.entity.tour.TravelCourse
import kr.hanchae.moyeotrip.entity.tour.TravelCourseType
import kr.hanchae.moyeotrip.entity.user.User
import kr.hanchae.moyeotrip.entity.user.UserRole
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 여행이 끝난 뒤 **채팅을 잠그는 것**(+7일)과 **보관·삭제**(+14일)는 다른 일이다.
 * 잠금이 삭제 예약을 지워 버리면 방이 영원히 남는다 — 그 경계를 고정한다.
 */
class ChatRoomCloseChatTest {
    @Test
    fun `채팅을 잠그면 더 쓸 수 없지만 삭제 예약은 그대로다`() {
        val room = confirmedRoom()
        room.scheduleDeletion(LocalDate.of(2026, 10, 1))

        room.closeChatForWriting(LocalDateTime.of(2026, 9, 24, 0, 15))

        assertFalse(room.canChat())
        assertEquals(LocalDate.of(2026, 10, 1), room.deletionScheduledDate)
    }

    @Test
    fun `이미 잠긴 방은 잠금 시각이 밀리지 않는다`() {
        // 스케줄러가 같은 방을 다시 집어도 「언제 잠겼는지」가 바뀌면 안 된다.
        val room = confirmedRoom()
        val first = LocalDateTime.of(2026, 9, 24, 0, 15)
        room.closeChatForWriting(first)

        room.closeChatForWriting(first.plusDays(3))

        assertEquals(first, room.chatClosedDateTime)
    }

    @Test
    fun `보관은 잠금과 달리 삭제 예약을 비운다`() {
        val room = confirmedRoom()
        room.scheduleDeletion(LocalDate.of(2026, 10, 1))

        room.archiveChat(LocalDateTime.of(2026, 10, 1, 0, 0))

        assertNull(room.deletionScheduledDate)
        assertTrue(room.isChatArchived())
    }

    @Test
    fun `잠그기 전에는 대화할 수 있다`() {
        val room = confirmedRoom()

        assertTrue(room.canChat())
        assertNull(room.chatClosedDateTime)
    }

    @Test
    fun `잠근 뒤에도 기록은 남는다`() {
        // 「더 못 쓴다」이지 「사라진다」가 아니다 — 읽기용 시각이 남아 있어야 한다.
        val room = confirmedRoom()
        room.closeChatForWriting(LocalDateTime.of(2026, 9, 24, 0, 15))

        assertNotNull(room.chatClosedDateTime)
    }

    private fun confirmedRoom(): ChatRoom =
        ChatRoom(
            id = 1L,
            host = User(id = 1L, userRole = UserRole.ROLE_USER),
            course = TravelCourse(id = 1L, type = TravelCourseType.CUSTOM, title = "테스트 코스"),
            roomTitle = "테스트 모임",
            maxParticipants = 5,
            minimumParticipants = 3,
            startDate = LocalDate.of(2026, 9, 16),
            endDate = LocalDate.of(2026, 9, 17),
            recruitmentDeadlineDate = LocalDate.of(2026, 9, 10),
            meetingDateTime = LocalDateTime.of(2026, 9, 16, 9, 0),
        ).apply { confirm() }
}
