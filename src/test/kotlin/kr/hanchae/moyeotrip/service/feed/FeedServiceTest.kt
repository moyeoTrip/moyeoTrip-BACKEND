package kr.hanchae.moyeotrip.service.feed

import kr.hanchae.moyeotrip.controller.feed.request.CreateFeedRequest
import kr.hanchae.moyeotrip.controller.feed.request.FeedTab
import kr.hanchae.moyeotrip.entity.chat.ChatRoom
import kr.hanchae.moyeotrip.entity.feed.Feed
import kr.hanchae.moyeotrip.entity.feed.FeedLike
import kr.hanchae.moyeotrip.entity.feed.FeedVisibility
import kr.hanchae.moyeotrip.entity.user.Gender
import kr.hanchae.moyeotrip.entity.user.NicknameColor
import kr.hanchae.moyeotrip.entity.user.User
import kr.hanchae.moyeotrip.entity.user.UserInformation
import kr.hanchae.moyeotrip.entity.user.UserRole
import kr.hanchae.moyeotrip.exception.BaseException
import kr.hanchae.moyeotrip.exception.ErrorCode
import kr.hanchae.moyeotrip.repository.ChatRoomParticipantRepository
import kr.hanchae.moyeotrip.repository.ChatRoomRepository
import kr.hanchae.moyeotrip.repository.FeedCommentRepository
import kr.hanchae.moyeotrip.repository.FeedLikeRepository
import kr.hanchae.moyeotrip.repository.FeedRepository
import kr.hanchae.moyeotrip.repository.FriendshipRepository
import kr.hanchae.moyeotrip.repository.ObjectStorageRepository
import kr.hanchae.moyeotrip.repository.UserBlockRepository
import kr.hanchae.moyeotrip.repository.UserRepository
import kr.hanchae.moyeotrip.service.notification.NotificationService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.springframework.mock.web.MockMultipartFile
import java.util.Optional

class FeedServiceTest {
    private val feedRepository = mock(FeedRepository::class.java)
    private val likeRepository = mock(FeedLikeRepository::class.java)
    private val commentRepository = mock(FeedCommentRepository::class.java)
    private val roomRepository = mock(ChatRoomRepository::class.java)
    private val participantRepository = mock(ChatRoomParticipantRepository::class.java)
    private val userRepository = mock(UserRepository::class.java)
    private val userBlockRepository = mock(UserBlockRepository::class.java)
    private val friendshipRepository = mock(FriendshipRepository::class.java)
    private val storageRepository = mock(ObjectStorageRepository::class.java)
    private val notificationService = mock(NotificationService::class.java)
    private val service =
        FeedService(
            feedRepository,
            likeRepository,
            commentRepository,
            roomRepository,
            participantRepository,
            userRepository,
            userBlockRepository,
            friendshipRepository,
            storageRepository,
            notificationService,
        )

    @Test
    fun `이미지가 아닌 파일로 피드를 작성할 수 없다`() {
        val file = MockMultipartFile("images", "memo.txt", "text/plain", "text".toByteArray())

        val exception =
            assertThrows(BaseException::class.java) {
                service.createFeed(1L, CreateFeedRequest(2L, "내용", FeedVisibility.PUBLIC), listOf(file))
            }

        assertEquals(ErrorCode.BAD_REQUEST, exception.errorCode)
        verifyNoInteractions(userRepository, roomRepository, storageRepository)
    }

    @Test
    fun `피드 사진은 열 장까지만 올릴 수 있다`() {
        val images =
            (1..11).map { index ->
                MockMultipartFile("images", "trip-$index.jpg", "image/jpeg", byteArrayOf(1))
            }

        val exception =
            assertThrows(BaseException::class.java) {
                service.createFeed(1L, CreateFeedRequest(2L, "내용", FeedVisibility.PUBLIC), images)
            }

        assertEquals(ErrorCode.BAD_REQUEST, exception.errorCode)
        verifyNoInteractions(userRepository, roomRepository, storageRepository)
    }

    @Test
    fun `완료한 여행의 참가자가 아니면 피드를 작성할 수 없다`() {
        val user = user(1L)
        val room = mock(ChatRoom::class.java)
        val image = MockMultipartFile("images", "trip.jpg", "image/jpeg", byteArrayOf(1))
        `when`(userRepository.findById(1L)).thenReturn(Optional.of(user))
        `when`(roomRepository.findById(2L)).thenReturn(Optional.of(room))
        `when`(room.id).thenReturn(2L)

        val exception =
            assertThrows(BaseException::class.java) {
                service.createFeed(1L, CreateFeedRequest(2L, "내용", FeedVisibility.PUBLIC), listOf(image))
            }

        assertEquals(ErrorCode.FORBIDDEN, exception.errorCode)
        verifyNoInteractions(storageRepository)
    }

    @Test
    fun `전체 피드는 차단 사용자가 제외된 무작위 조회를 사용한다`() {
        `when`(feedRepository.findRandomDiscoverFeeds(1L, 20)).thenReturn(emptyList())

        val response = service.getFeeds(1L, FeedTab.DISCOVER, beforeFeedId = 100L, limit = 20)

        assertEquals(emptyList<Any>(), response.feeds)
        assertEquals(null, response.nextId)
        verify(feedRepository).findRandomDiscoverFeeds(1L, 20)
    }

    @Test
    fun `좋아요 상태를 토글해 취소한다`() {
        val like = mock(FeedLike::class.java)
        val feed = mock(Feed::class.java)
        `when`(feed.author).thenReturn(user(2L))
        `when`(feed.visibility).thenReturn(FeedVisibility.PUBLIC)
        `when`(feedRepository.findById(3L)).thenReturn(Optional.of(feed))
        `when`(likeRepository.countByFeedId(3L)).thenReturn(5L)
        `when`(likeRepository.findByFeedIdAndUserId(3L, 1L)).thenReturn(like)

        val response = service.toggleLike(1L, 3L)

        assertEquals(false, response.liked)
        assertEquals(4L, response.likeCount)
        verify(likeRepository).delete(like)
        verifyNoInteractions(userRepository)
    }

    @Test
    fun `피드에 좋아요를 누르면 작성자 알림을 요청한다`() {
        val feed = mock(Feed::class.java)
        val user = user(1L)
        `when`(feed.author).thenReturn(user(2L))
        `when`(feed.visibility).thenReturn(FeedVisibility.PUBLIC)
        `when`(likeRepository.countByFeedId(3L)).thenReturn(2L)
        `when`(likeRepository.findByFeedIdAndUserId(3L, 1L)).thenReturn(null)
        `when`(feedRepository.findById(3L)).thenReturn(Optional.of(feed))
        `when`(userRepository.findById(1L)).thenReturn(Optional.of(user))

        val response = service.toggleLike(1L, 3L)

        assertEquals(true, response.liked)
        assertEquals(3L, response.likeCount)
        verify(notificationService).notifyFeedLiked(feed, user)
    }

    @Test
    fun `나만 보기 피드는 작성자 외에는 조회할 수 없다`() {
        val feed = mock(Feed::class.java)
        `when`(feed.author).thenReturn(user(2L))
        `when`(feed.visibility).thenReturn(FeedVisibility.PRIVATE)
        `when`(feedRepository.findById(3L)).thenReturn(Optional.of(feed))

        val exception = assertThrows(BaseException::class.java) { service.getFeed(1L, 3L) }

        assertEquals(ErrorCode.FORBIDDEN, exception.errorCode)
    }

    @Test
    fun `친구 공개 피드는 친구만 조회할 수 있다`() {
        val feed = mock(Feed::class.java)
        val like = mock(FeedLike::class.java)
        `when`(feed.author).thenReturn(user(2L))
        `when`(feed.visibility).thenReturn(FeedVisibility.FRIENDS)
        `when`(feedRepository.findById(3L)).thenReturn(Optional.of(feed))
        `when`(friendshipRepository.existsBetween(1L, 2L)).thenReturn(true)
        `when`(likeRepository.findByFeedIdAndUserId(3L, 1L)).thenReturn(like)

        val response = service.toggleLike(1L, 3L)

        assertEquals(false, response.liked)
        verify(friendshipRepository).existsBetween(1L, 2L)
    }

    private fun user(id: Long): User =
        User(
            id = id,
            userRole = UserRole.ROLE_USER,
            userInformation = UserInformation("여행자", NicknameColor.GREEN, Gender.N),
        )
}
