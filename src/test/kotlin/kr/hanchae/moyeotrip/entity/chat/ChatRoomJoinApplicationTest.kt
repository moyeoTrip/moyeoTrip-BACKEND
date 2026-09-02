package kr.hanchae.moyeotrip.entity.chat

import kr.hanchae.moyeotrip.entity.user.User
import kr.hanchae.moyeotrip.entity.user.UserRole
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock

class ChatRoomJoinApplicationTest {
    @Test
    fun `승인 대기 신청은 대기열로 이동하거나 거절할 수 있다`() {
        val waitlisted = application()
        val rejected = application()

        waitlisted.moveToWaitlist()
        rejected.reject()

        assertEquals(JoinApplicationStatus.WAITLISTED, waitlisted.status)
        assertEquals(JoinApplicationStatus.REJECTED, rejected.status)
    }

    @Test
    fun `이미 처리한 신청은 다시 상태를 바꿀 수 없다`() {
        val waitlisted = application().also { it.moveToWaitlist() }
        val rejected = application().also { it.reject() }

        assertThrows(IllegalStateException::class.java) { waitlisted.moveToWaitlist() }
        assertThrows(IllegalStateException::class.java) { waitlisted.reject() }
        assertThrows(IllegalStateException::class.java) { rejected.moveToWaitlist() }
        assertThrows(IllegalStateException::class.java) { rejected.reject() }
    }

    private fun application() =
        ChatRoomJoinApplication(
            chatRoom = mock(ChatRoom::class.java),
            user = User(id = 2L, userRole = UserRole.ROLE_USER),
            applicationMessage = "함께 여행하고 싶어요",
        )
}
