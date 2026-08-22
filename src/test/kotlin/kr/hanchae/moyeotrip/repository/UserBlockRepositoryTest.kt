package kr.hanchae.moyeotrip.repository

import kr.hanchae.moyeotrip.entity.user.UserBlock
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class UserBlockRepositoryTest : RepositoryIntegrationTestSupport() {
    @Autowired
    private lateinit var userBlockRepository: UserBlockRepository

    @Nested
    inner class RelatedUserQueries {
        @Test
        fun `상대 사용자 ID만 반환하고 양방향 차단 여부를 확인한다`() {
            val first = savedUser()
            val second = savedUser()
            val third = savedUser()
            val fourth = savedUser()
            userBlockRepository.saveAndFlush(UserBlock(blocker = first, blocked = second))
            userBlockRepository.saveAndFlush(UserBlock(blocker = third, blocked = first))
            userBlockRepository.saveAndFlush(UserBlock(blocker = third, blocked = fourth))

            assertEquals(setOf(second.id, third.id), userBlockRepository.findRelatedUserIds(first.id).toSet())
            assertTrue(userBlockRepository.existsBetween(second.id, first.id))
            assertFalse(userBlockRepository.existsBetween(second.id, fourth.id))
        }
    }
}
