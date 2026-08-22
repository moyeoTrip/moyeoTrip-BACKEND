package kr.hanchae.moyeotrip.repository

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

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
}
