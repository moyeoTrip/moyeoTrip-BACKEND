package kr.hanchae.moyeotrip.repository

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class UserRepositoryTest : RepositoryIntegrationTestSupport() {
    @Nested
    inner class FindByIdForUpdate {
        @Test
        fun `사용자를 비관적 락 조회로 반환한다`() {
            val user = savedUser()

            assertEquals(user.id, userRepository.findByIdForUpdate(user.id)?.id)
            assertNull(userRepository.findByIdForUpdate(Long.MAX_VALUE))
        }
    }

    @Test
    fun `영구 삭제 기준 시각이 지난 탈퇴 사용자만 조회한다`() {
        val now = LocalDateTime.of(2026, 8, 31, 12, 0)
        val expired = savedUser().also { it.withdraw(now.minusDays(30)) }
        savedUser().also { it.withdraw(now.minusDays(29)) }
        savedUser()
        userRepository.flush()

        val users = userRepository.findAllByWithdrawnDateTimeLessThanEqual(now.minusDays(30))

        assertEquals(listOf(expired.id), users.map { it.id })
    }
}
