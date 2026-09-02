package kr.hanchae.moyeotrip.entity.user

import kr.hanchae.moyeotrip.entity.chat.ChatRoom
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import java.time.LocalDateTime

class TravelCompanionTest {
    @Test
    fun `평가가 없으면 평가 시각도 없고 평가하면 값과 시각을 기록한다`() {
        val companion = companion()

        assertNull(companion.mannerScore)
        assertNull(companion.oneLineReview)
        assertNull(companion.reviewedAt)

        companion.review(5, "약속을 잘 지켜요")

        assertEquals(5, companion.mannerScore)
        assertEquals("약속을 잘 지켜요", companion.oneLineReview)
        assertNotNull(companion.reviewedAt)
    }

    @Test
    fun `초기 평가나 한 줄 평이 있으면 평가 시각을 자동 기록한다`() {
        assertNotNull(companion(mannerScore = 3).reviewedAt)
        assertNotNull(companion(oneLineReview = "좋은 여행자예요").reviewedAt)
        assertNull(companion(oneLineReview = "   ").reviewedAt)

        val at = LocalDateTime.of(2026, 8, 30, 10, 0)
        assertEquals(at, companion(reviewedAt = at).reviewedAt)
    }

    @Test
    fun `자기 자신이나 잘못된 평가 값은 거부한다`() {
        assertThrows(IllegalArgumentException::class.java) { companion(ownerId = 1L, companionId = 1L) }
        assertThrows(IllegalArgumentException::class.java) { companion(mannerScore = 0) }
        assertThrows(IllegalArgumentException::class.java) { companion(mannerScore = 6) }
        assertThrows(IllegalArgumentException::class.java) { companion(oneLineReview = "가".repeat(41)) }
    }

    @Test
    fun `평가 수정도 점수와 한 줄 평 길이를 검증한다`() {
        val companion = companion()

        assertThrows(IllegalArgumentException::class.java) { companion.review(0, null) }
        assertThrows(IllegalArgumentException::class.java) { companion.review(6, null) }
        assertThrows(IllegalArgumentException::class.java) { companion.review(3, "가".repeat(41)) }
        companion.review(1, null)

        assertEquals(1, companion.mannerScore)
        assertNull(companion.oneLineReview)
    }

    private fun companion(
        ownerId: Long = 1L,
        companionId: Long = 2L,
        mannerScore: Int? = null,
        oneLineReview: String? = null,
        reviewedAt: LocalDateTime? = null,
    ) = TravelCompanion(
        owner = User(id = ownerId, userRole = UserRole.ROLE_USER),
        companion = User(id = companionId, userRole = UserRole.ROLE_USER),
        chatRoom = mock(ChatRoom::class.java),
        mannerScore = mannerScore,
        oneLineReview = oneLineReview,
        reviewedAt = reviewedAt,
    )
}
