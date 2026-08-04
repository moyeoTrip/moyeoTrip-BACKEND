package kr.hanchae.moyeotrip.entity.user

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class UserMannerRatingTest {
    @Test
    fun `평가받은 적이 없는 사용자의 매너 평점은 없다`() {
        val user = User(id = 1L, userRole = UserRole.ROLE_USER)

        assertNull(user.mannerRating)
    }

    @Test
    fun `매너 평점은 소수점 첫째 자리로 반올림한다`() {
        val user = User(id = 1L, userRole = UserRole.ROLE_USER)

        user.updateMannerRating(4.46)

        assertEquals(4.5, user.mannerRating)
    }
}
