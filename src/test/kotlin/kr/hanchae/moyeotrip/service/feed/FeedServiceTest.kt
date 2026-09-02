package kr.hanchae.moyeotrip.service.feed

import kr.hanchae.moyeotrip.controller.feed.request.CreateFeedCommentRequest
import kr.hanchae.moyeotrip.controller.feed.request.CreateFeedReportRequest
import kr.hanchae.moyeotrip.controller.feed.request.CreateFeedRequest
import kr.hanchae.moyeotrip.controller.feed.request.FeedTab
import kr.hanchae.moyeotrip.entity.chat.ChatRoom
import kr.hanchae.moyeotrip.entity.feed.Feed
import kr.hanchae.moyeotrip.entity.feed.FeedComment
import kr.hanchae.moyeotrip.entity.feed.FeedLike
import kr.hanchae.moyeotrip.entity.feed.FeedReport
import kr.hanchae.moyeotrip.entity.feed.FeedReportReason
import kr.hanchae.moyeotrip.entity.feed.FeedVisibility
import kr.hanchae.moyeotrip.entity.tour.TravelCourse
import kr.hanchae.moyeotrip.entity.tour.TravelCourseType
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
import kr.hanchae.moyeotrip.repository.FeedReportRepository
import kr.hanchae.moyeotrip.repository.FeedRepository
import kr.hanchae.moyeotrip.repository.FriendshipRepository
import kr.hanchae.moyeotrip.repository.ObjectStorageRepository
import kr.hanchae.moyeotrip.repository.UserBlockRepository
import kr.hanchae.moyeotrip.repository.UserRepository
import kr.hanchae.moyeotrip.service.notification.NotificationService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.springframework.data.domain.PageRequest
import org.springframework.mock.web.MockMultipartFile
import java.time.LocalDate
import java.time.LocalDateTime
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
    private val reportRepository = mock(FeedReportRepository::class.java)
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
            reportRepository,
        )

    @Test
    fun `신고 사유 목록을 표시명과 함께 반환한다`() {
        val response = service.getReportReasons()

        assertEquals(FeedReportReason.entries, response.map { it.reason })
        assertEquals("스팸 또는 광고", response.first().displayName)
        assertEquals(
            "돈거래 유도",
            response.single { it.reason == FeedReportReason.MONEY_TRANSACTION_SOLICITATION }.displayName,
        )
    }

    @Test
    fun `세 번째 신고가 누적되면 피드를 비공개 처리한다`() {
        val author = user(2L)
        val reporter = user(1L)
        val feed =
            Feed(
                id = 3L,
                author = author,
                chatRoom = mock(ChatRoom::class.java),
                content = "신고 대상",
                visibility = FeedVisibility.PUBLIC,
            )
        `when`(feedRepository.findByIdForUpdate(3L)).thenReturn(feed)
        `when`(userRepository.findById(1L)).thenReturn(Optional.of(reporter))
        `when`(reportRepository.existsByFeedIdAndReporterId(3L, 1L)).thenReturn(false)
        `when`(reportRepository.countByFeedId(3L)).thenReturn(3L)

        service.reportFeed(1L, 3L, CreateFeedReportRequest(FeedReportReason.SPAM, " 반복 광고 "))

        assertEquals(FeedVisibility.PRIVATE, feed.visibility)
        assertTrue(feed.hiddenByReports)
        verify(reportRepository).saveAndFlush(org.mockito.ArgumentMatchers.any(FeedReport::class.java))
    }

    @Test
    fun `같은 사용자는 동일 피드를 중복 신고할 수 없다`() {
        val feed = mock(Feed::class.java)
        `when`(feed.author).thenReturn(user(2L))
        `when`(feed.visibility).thenReturn(FeedVisibility.PUBLIC)
        `when`(feedRepository.findByIdForUpdate(3L)).thenReturn(feed)
        `when`(reportRepository.existsByFeedIdAndReporterId(3L, 1L)).thenReturn(true)

        val exception =
            assertThrows(BaseException::class.java) {
                service.reportFeed(1L, 3L, CreateFeedReportRequest(FeedReportReason.SPAM))
            }

        assertEquals(ErrorCode.FEED_ALREADY_REPORTED, exception.errorCode)
        verifyNoInteractions(userRepository)
    }

    @Test
    fun `이미지가 아닌 파일로 피드를 작성할 수 없다`() {
        val file = MockMultipartFile("images", "memo.txt", "text/plain", "text".toByteArray())

        val exception =
            assertThrows(BaseException::class.java) {
                service.createFeed(1L, CreateFeedRequest(2L, "내용", FeedVisibility.PUBLIC), listOf(file))
            }

        assertEquals(ErrorCode.INVALID_FEED_IMAGE, exception.errorCode)
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

        assertEquals(ErrorCode.INVALID_FEED_IMAGE_COUNT, exception.errorCode)
        verifyNoInteractions(userRepository, roomRepository, storageRepository)
    }

    @Test
    fun `사진이 없거나 20MB를 초과하면 피드를 작성할 수 없다`() {
        val oversized = mock(org.springframework.web.multipart.MultipartFile::class.java)
        `when`(oversized.isEmpty).thenReturn(false)
        `when`(oversized.size).thenReturn(20L * 1024L * 1024L + 1L)
        `when`(oversized.contentType).thenReturn("image/jpeg")

        val emptyException =
            assertThrows(BaseException::class.java) {
                service.createFeed(1L, CreateFeedRequest(2L, "내용", FeedVisibility.PUBLIC), emptyList())
            }
        val oversizedException =
            assertThrows(BaseException::class.java) {
                service.createFeed(1L, CreateFeedRequest(2L, "내용", FeedVisibility.PUBLIC), listOf(oversized))
            }

        assertEquals(ErrorCode.INVALID_FEED_IMAGE_COUNT, emptyException.errorCode)
        assertEquals(ErrorCode.INVALID_FEED_IMAGE, oversizedException.errorCode)
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

        assertEquals(ErrorCode.COMPLETED_TRIP_FEED_REQUIRED, exception.errorCode)
        verifyNoInteractions(storageRepository)
    }

    @Test
    fun `같은 여행에는 피드를 한 번만 작성할 수 있다`() {
        val image = MockMultipartFile("images", "trip.jpg", "image/jpeg", byteArrayOf(1))
        val room = mock(ChatRoom::class.java)
        `when`(room.id).thenReturn(2L)
        `when`(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)))
        `when`(roomRepository.findById(2L)).thenReturn(Optional.of(room))
        `when`(participantRepository.hasCompletedTrip(2L, 1L, LocalDate.now())).thenReturn(true)
        `when`(feedRepository.existsByChatRoomIdAndAuthorId(2L, 1L)).thenReturn(true)

        val exception =
            assertThrows(BaseException::class.java) {
                service.createFeed(1L, CreateFeedRequest(2L, "내용", FeedVisibility.PUBLIC), listOf(image))
            }

        assertEquals(ErrorCode.FEED_ALREADY_CREATED_FOR_TRIP, exception.errorCode)
        verifyNoInteractions(storageRepository)
    }

    @Test
    fun `두 번째 사진 업로드가 실패하면 앞서 업로드한 사진을 삭제한다`() {
        val first = MockMultipartFile("images", "first.PNG", "image/png", byteArrayOf(1))
        val second = MockMultipartFile("images", "second.exe", "image/jpeg", byteArrayOf(2))
        val room = mock(ChatRoom::class.java)
        `when`(room.id).thenReturn(2L)
        `when`(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)))
        `when`(roomRepository.findById(2L)).thenReturn(Optional.of(room))
        `when`(participantRepository.hasCompletedTrip(2L, 1L, LocalDate.now())).thenReturn(true)
        `when`(
            storageRepository.upload(
                anyValue(),
                anyValue(),
                anyValue(),
            ),
        ).thenReturn("first-key").thenThrow(IllegalStateException("upload failed"))

        assertThrows(IllegalStateException::class.java) {
            service.createFeed(1L, CreateFeedRequest(2L, "내용", FeedVisibility.PUBLIC), listOf(first, second))
        }

        verify(storageRepository).delete("first-key")
        verifyNoInteractions(notificationService)
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
    fun `친구 피드는 커서와 최대 페이지 크기를 적용한다`() {
        val feeds = (3L downTo 1L).map(::responseFeed)
        `when`(
            feedRepository.findFriendFeeds(
                1L,
                Long.MAX_VALUE,
                FeedVisibility.PUBLIC,
                FeedVisibility.FRIENDS,
                PageRequest.of(0, 3),
            ),
        ).thenReturn(feeds)

        val response = service.getFeeds(1L, FeedTab.FRIENDS, beforeFeedId = null, limit = 2)

        assertEquals(listOf(3L, 2L), response.feeds.map { it.feedId })
        assertEquals(2L, response.nextId)
    }

    @Test
    fun `친구 피드가 페이지 크기 이하이면 다음 커서가 없고 최소 페이지 크기를 적용한다`() {
        `when`(
            feedRepository.findFriendFeeds(
                1L,
                50L,
                FeedVisibility.PUBLIC,
                FeedVisibility.FRIENDS,
                PageRequest.of(0, 2),
            ),
        ).thenReturn(emptyList())

        val response = service.getFeeds(1L, FeedTab.FRIENDS, beforeFeedId = 50L, limit = 0)

        assertTrue(response.feeds.isEmpty())
        assertEquals(null, response.nextId)
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
    fun `좋아요 수가 이미 0이면 취소해도 음수가 되지 않는다`() {
        val like = mock(FeedLike::class.java)
        val feed = mock(Feed::class.java)
        `when`(feed.author).thenReturn(user(1L))
        `when`(feed.visibility).thenReturn(FeedVisibility.PRIVATE)
        `when`(feedRepository.findById(3L)).thenReturn(Optional.of(feed))
        `when`(likeRepository.countByFeedId(3L)).thenReturn(0L)
        `when`(likeRepository.findByFeedIdAndUserId(3L, 1L)).thenReturn(like)

        val response = service.toggleLike(1L, 3L)

        assertEquals(0L, response.likeCount)
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

        assertEquals(ErrorCode.FEED_NOT_VISIBLE_TO_USER, exception.errorCode)
    }

    @Test
    fun `신고로 비공개된 피드는 작성자가 조회하면 신고 비공개 여부를 내려준다`() {
        val feed = responseFeed(3L)
        `when`(feed.visibility).thenReturn(FeedVisibility.PRIVATE)
        `when`(feed.hiddenByReports).thenReturn(true)
        `when`(feedRepository.findById(3L)).thenReturn(Optional.of(feed))

        val response = service.getFeed(2L, 3L)

        assertTrue(response.hiddenByReports)
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

    @Test
    fun `친구 공개 피드는 작성자 본인에게 친구 조회 없이 노출된다`() {
        val feed = responseFeed(3L)
        `when`(feed.visibility).thenReturn(FeedVisibility.FRIENDS)
        `when`(feedRepository.findById(3L)).thenReturn(Optional.of(feed))

        val response = service.getFeed(2L, 3L)

        assertEquals(3L, response.feedId)
        verifyNoInteractions(friendshipRepository)
    }

    @Test
    fun `친구가 아닌 사용자는 친구 공개 피드를 조회할 수 없다`() {
        val feed = mock(Feed::class.java)
        `when`(feed.author).thenReturn(user(2L))
        `when`(feed.visibility).thenReturn(FeedVisibility.FRIENDS)
        `when`(feedRepository.findById(3L)).thenReturn(Optional.of(feed))
        `when`(friendshipRepository.existsBetween(1L, 2L)).thenReturn(false)

        val exception = assertThrows(BaseException::class.java) { service.getFeed(1L, 3L) }

        assertEquals(ErrorCode.FEED_NOT_VISIBLE_TO_USER, exception.errorCode)
    }

    @Test
    fun `자신의 피드는 신고할 수 없다`() {
        val feed = mock(Feed::class.java)
        `when`(feed.author).thenReturn(user(1L))
        `when`(feedRepository.findByIdForUpdate(3L)).thenReturn(feed)

        val exception =
            assertThrows(BaseException::class.java) {
                service.reportFeed(1L, 3L, CreateFeedReportRequest(FeedReportReason.SPAM, null))
            }

        assertEquals(ErrorCode.SELF_FEED_REPORT_NOT_ALLOWED, exception.errorCode)
        verifyNoInteractions(reportRepository)
    }

    @Test
    fun `차단 관계인 사용자의 공개 피드는 조회할 수 없다`() {
        val feed = mock(Feed::class.java)
        `when`(feed.author).thenReturn(user(2L))
        `when`(feedRepository.findById(3L)).thenReturn(Optional.of(feed))
        `when`(userBlockRepository.existsBetween(1L, 2L)).thenReturn(true)

        val exception = assertThrows(BaseException::class.java) { service.getFeed(1L, 3L) }

        assertEquals(ErrorCode.USER_BLOCK_RELATIONSHIP, exception.errorCode)
        verifyNoInteractions(friendshipRepository)
    }

    @Test
    fun `대댓글의 부모가 다른 피드이거나 이미 대댓글이면 작성할 수 없다`() {
        val feed = mock(Feed::class.java)
        `when`(feed.author).thenReturn(user(1L))
        `when`(feed.visibility).thenReturn(FeedVisibility.PRIVATE)
        `when`(feedRepository.findById(3L)).thenReturn(Optional.of(feed))
        `when`(commentRepository.findByIdAndFeedId(5L, 3L)).thenReturn(null)

        val exception =
            assertThrows(BaseException::class.java) {
                service.createComment(1L, 3L, CreateFeedCommentRequest("답글", parentCommentId = 5L))
            }

        assertEquals(ErrorCode.FEED_PARENT_COMMENT_NOT_FOUND, exception.errorCode)
        verifyNoInteractions(userRepository)
    }

    @Test
    fun `대댓글은 최상위 부모 댓글 아래에 저장한다`() {
        val author = user(1L)
        val feed = mock(Feed::class.java)
        val parent = mock(FeedComment::class.java)
        val savedComment = mock(FeedComment::class.java)
        `when`(feed.author).thenReturn(author)
        `when`(feed.visibility).thenReturn(FeedVisibility.PRIVATE)
        `when`(feedRepository.findById(3L)).thenReturn(Optional.of(feed))
        `when`(commentRepository.findByIdAndFeedId(5L, 3L)).thenReturn(parent)
        `when`(parent.parent).thenReturn(null)
        `when`(userRepository.findById(1L)).thenReturn(Optional.of(author))
        `when`(savedComment.id).thenReturn(7L)
        `when`(savedComment.author).thenReturn(author)
        `when`(savedComment.content).thenReturn("답글")
        `when`(savedComment.createdDateTime).thenReturn(LocalDateTime.now())
        `when`(commentRepository.save(org.mockito.ArgumentMatchers.any(FeedComment::class.java))).thenReturn(savedComment)

        val response = service.createComment(1L, 3L, CreateFeedCommentRequest(" 답글 ", parentCommentId = 5L))

        assertEquals("답글", response.content)
        val captor = ArgumentCaptor.forClass(FeedComment::class.java)
        verify(commentRepository).save(captor.capture())
        assertEquals(parent, captor.value.parent)
    }

    @Test
    fun `대댓글에 다시 답글을 작성할 수 없다`() {
        val author = user(1L)
        val feed = mock(Feed::class.java)
        val root = mock(FeedComment::class.java)
        val reply = mock(FeedComment::class.java)
        `when`(feed.author).thenReturn(author)
        `when`(feed.visibility).thenReturn(FeedVisibility.PRIVATE)
        `when`(feedRepository.findById(3L)).thenReturn(Optional.of(feed))
        `when`(commentRepository.findByIdAndFeedId(5L, 3L)).thenReturn(reply)
        `when`(reply.parent).thenReturn(root)

        val exception =
            assertThrows(BaseException::class.java) {
                service.createComment(1L, 3L, CreateFeedCommentRequest("중첩 답글", parentCommentId = 5L))
            }

        assertEquals(ErrorCode.FEED_PARENT_COMMENT_NOT_FOUND, exception.errorCode)
    }

    @Test
    fun `최상위 댓글은 공백을 제거해 저장한다`() {
        val author = user(1L)
        val feed = mock(Feed::class.java)
        val savedComment = mock(FeedComment::class.java)
        `when`(feed.author).thenReturn(author)
        `when`(feed.visibility).thenReturn(FeedVisibility.PRIVATE)
        `when`(feedRepository.findById(3L)).thenReturn(Optional.of(feed))
        `when`(userRepository.findById(1L)).thenReturn(Optional.of(author))
        `when`(savedComment.id).thenReturn(7L)
        `when`(savedComment.author).thenReturn(author)
        `when`(savedComment.content).thenReturn("댓글")
        `when`(savedComment.createdDateTime).thenReturn(LocalDateTime.now())
        `when`(commentRepository.save(org.mockito.ArgumentMatchers.any(FeedComment::class.java))).thenReturn(savedComment)

        val response = service.createComment(1L, 3L, CreateFeedCommentRequest(" 댓글 "))

        assertEquals("댓글", response.content)
    }

    private fun responseFeed(id: Long): Feed {
        val feed = mock(Feed::class.java)
        val room = mock(ChatRoom::class.java)
        `when`(feed.id).thenReturn(id)
        `when`(feed.author).thenReturn(user(2L))
        `when`(feed.chatRoom).thenReturn(room)
        `when`(feed.content).thenReturn("피드 $id")
        `when`(feed.visibility).thenReturn(FeedVisibility.PUBLIC)
        `when`(feed.images).thenReturn(emptyList())
        `when`(feed.createdDateTime).thenReturn(LocalDateTime.now())
        `when`(room.id).thenReturn(10L)
        `when`(room.course).thenReturn(TravelCourse(id = 20L, type = TravelCourseType.PUBLIC, title = "코스"))
        `when`(room.startDate).thenReturn(LocalDate.now())
        return feed
    }

    private fun user(id: Long): User =
        User(
            id = id,
            userRole = UserRole.ROLE_USER,
            userInformation = UserInformation("여행자", NicknameColor.GREEN, Gender.N),
        )

    @Suppress("UNCHECKED_CAST")
    private fun <T> anyValue(): T = org.mockito.Mockito.any<T>()
}
