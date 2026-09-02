package kr.hanchae.moyeotrip.entity.tour

import kr.hanchae.moyeotrip.entity.chat.ChatRoom
import kr.hanchae.moyeotrip.entity.user.User
import kr.hanchae.moyeotrip.entity.user.UserRole
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock

class TravelCourseRatingTest {
    @Test
    fun `코스 평점은 1점부터 5점까지 생성하고 수정할 수 있다`() {
        val minimum = rating(1)
        val maximum = rating(5)

        minimum.update(5)
        maximum.update(1)

        assertEquals(5, minimum.score)
        assertEquals(1, maximum.score)
    }

    @Test
    fun `코스 평점 범위를 벗어나면 생성과 수정을 거부한다`() {
        assertThrows(IllegalArgumentException::class.java) { rating(0) }
        assertThrows(IllegalArgumentException::class.java) { rating(6) }

        val rating = rating(3)
        assertThrows(IllegalArgumentException::class.java) { rating.update(0) }
        assertThrows(IllegalArgumentException::class.java) { rating.update(6) }
        assertEquals(3, rating.score)
    }

    private fun rating(score: Int) =
        TravelCourseRating(
            course = TravelCourse(id = 1L, type = TravelCourseType.PUBLIC, title = "공개 코스"),
            chatRoom = mock(ChatRoom::class.java),
            user = User(id = 1L, userRole = UserRole.ROLE_USER),
            score = score,
        )
}
