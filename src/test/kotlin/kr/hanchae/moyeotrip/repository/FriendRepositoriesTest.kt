package kr.hanchae.moyeotrip.repository

import kr.hanchae.moyeotrip.entity.user.FriendRequest
import kr.hanchae.moyeotrip.entity.user.Friendship
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class FriendRepositoriesTest : RepositoryIntegrationTestSupport() {
    @Autowired
    private lateinit var friendshipRepository: FriendshipRepository

    @Autowired
    private lateinit var friendRequestRepository: FriendRequestRepository

    @Nested
    inner class FriendshipQueries {
        @Test
        fun `양방향 친구 관계를 조회하고 삭제한다`() {
            val first = savedUser()
            val second = savedUser()
            val third = savedUser()
            val fourth = savedUser()
            val firstSecond = friendshipRepository.saveAndFlush(Friendship(firstUser = first, secondUser = second))
            val firstThird = friendshipRepository.saveAndFlush(Friendship(firstUser = first, secondUser = third))
            friendshipRepository.saveAndFlush(Friendship(firstUser = third, secondUser = fourth))

            assertEquals(firstSecond.id, friendshipRepository.findBetween(second.id, first.id)?.id)
            assertTrue(friendshipRepository.existsBetween(second.id, first.id))
            assertFalse(friendshipRepository.existsBetween(second.id, third.id))
            assertEquals(
                setOf(firstSecond.id, firstThird.id),
                friendshipRepository.findAllByUserId(first.id).map { it.id }.toSet(),
            )

            assertEquals(1, friendshipRepository.deleteBetween(second.id, first.id))
            assertFalse(friendshipRepository.existsBetween(first.id, second.id))
        }
    }

    @Nested
    inner class FriendRequestQueries {
        @Test
        fun `양방향 친구 요청을 함께 삭제한다`() {
            val first = savedUser()
            val second = savedUser()
            val otherUser = savedUser()
            friendRequestRepository.saveAndFlush(FriendRequest(requester = first, receiver = second))
            friendRequestRepository.saveAndFlush(FriendRequest(requester = second, receiver = first))
            val unrelatedRequest = friendRequestRepository.saveAndFlush(FriendRequest(requester = first, receiver = otherUser))

            assertEquals(2, friendRequestRepository.deleteBetween(first.id, second.id))
            assertEquals(listOf(unrelatedRequest.id), friendRequestRepository.findAll().map { it.id })
        }
    }
}
