package kr.hanchae.moyeotrip.repository

import kr.hanchae.moyeotrip.entity.feed.Feed
import kr.hanchae.moyeotrip.entity.feed.FeedVisibility
import kr.hanchae.moyeotrip.entity.user.Friendship
import kr.hanchae.moyeotrip.entity.user.UserBlock
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest

class FeedRepositoryTest : RepositoryIntegrationTestSupport() {
    @Autowired
    private lateinit var feedRepository: FeedRepository

    @Autowired
    private lateinit var friendshipRepository: FriendshipRepository

    @Autowired
    private lateinit var userBlockRepository: UserBlockRepository

    @Nested
    inner class DiscoverFeedQueries {
        @Test
        fun `공개 피드 중 차단 관계를 제외하고 무작위로 조회한다`() {
            val fixture = feedFixture()

            val feeds = feedRepository.findRandomDiscoverFeeds(fixture.viewerId, 20)

            assertEquals(setOf(fixture.friendPublicId, fixture.strangerPublicId), feeds.map { it.id }.toSet())
        }
    }

    @Nested
    inner class FriendFeedQueries {
        @Test
        fun `친구의 전체와 친구 공개 피드만 조회한다`() {
            val fixture = feedFixture()

            val feeds =
                feedRepository.findFriendFeeds(
                    userId = fixture.viewerId,
                    beforeId = Long.MAX_VALUE,
                    publicVisibility = FeedVisibility.PUBLIC,
                    friendsVisibility = FeedVisibility.FRIENDS,
                    pageable = PageRequest.of(0, 20),
                )

            assertEquals(setOf(fixture.friendPublicId, fixture.friendOnlyId), feeds.map { it.id }.toSet())
            assertFalse(feeds.any { it.id == fixture.blockedPublicId })
        }
    }

    private fun feedFixture(): FeedFixture {
        val viewer = savedUser()
        val friend = savedUser()
        val blockedAuthor = savedUser()
        val stranger = savedUser()
        val course = savedCourse()
        val viewerRoom = savedRoom(viewer, course, title = "뷰어 방")
        val friendRoom = savedRoom(friend, course, title = "친구 방")
        val friendOnlyRoom = savedRoom(friend, course, title = "친구 전용 방")
        val friendPrivateRoom = savedRoom(friend, course, title = "친구 비공개 방")
        val blockedRoom = savedRoom(blockedAuthor, course, title = "차단 방")
        val strangerRoom = savedRoom(stranger, course, title = "낯선 방")
        friendshipRepository.saveAndFlush(Friendship(firstUser = viewer, secondUser = friend))
        userBlockRepository.saveAndFlush(UserBlock(blocker = viewer, blocked = blockedAuthor))

        val friendPublic =
            feedRepository.saveAndFlush(
                Feed(author = friend, chatRoom = friendRoom, content = "친구 전체", visibility = FeedVisibility.PUBLIC),
            )
        val friendOnly =
            feedRepository.saveAndFlush(
                Feed(author = friend, chatRoom = friendOnlyRoom, content = "친구 공개", visibility = FeedVisibility.FRIENDS),
            )
        feedRepository.saveAndFlush(
            Feed(author = friend, chatRoom = friendPrivateRoom, content = "친구 비공개", visibility = FeedVisibility.PRIVATE),
        )
        val blockedPublic =
            feedRepository.saveAndFlush(
                Feed(author = blockedAuthor, chatRoom = blockedRoom, content = "차단", visibility = FeedVisibility.PUBLIC),
            )
        val strangerPublic =
            feedRepository.saveAndFlush(
                Feed(author = stranger, chatRoom = strangerRoom, content = "낯선 사람", visibility = FeedVisibility.PUBLIC),
            )
        feedRepository.saveAndFlush(
            Feed(author = viewer, chatRoom = viewerRoom, content = "비공개", visibility = FeedVisibility.PRIVATE),
        )

        return FeedFixture(
            viewerId = viewer.id,
            friendPublicId = friendPublic.id,
            friendOnlyId = friendOnly.id,
            blockedPublicId = blockedPublic.id,
            strangerPublicId = strangerPublic.id,
        )
    }

    private data class FeedFixture(
        val viewerId: Long,
        val friendPublicId: Long,
        val friendOnlyId: Long,
        val blockedPublicId: Long,
        val strangerPublicId: Long,
    )
}
