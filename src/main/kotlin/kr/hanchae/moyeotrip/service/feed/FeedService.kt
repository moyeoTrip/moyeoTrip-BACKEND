package kr.hanchae.moyeotrip.service.feed

import kr.hanchae.moyeotrip.controller.feed.request.CreateFeedCommentRequest
import kr.hanchae.moyeotrip.controller.feed.request.CreateFeedRequest
import kr.hanchae.moyeotrip.controller.feed.request.FeedTab
import kr.hanchae.moyeotrip.controller.feed.response.FeedAuthorResponse
import kr.hanchae.moyeotrip.controller.feed.response.FeedCommentResponse
import kr.hanchae.moyeotrip.controller.feed.response.FeedImageResponse
import kr.hanchae.moyeotrip.controller.feed.response.FeedLikeResponse
import kr.hanchae.moyeotrip.controller.feed.response.FeedPageResponse
import kr.hanchae.moyeotrip.controller.feed.response.FeedPlaceResponse
import kr.hanchae.moyeotrip.controller.feed.response.FeedResponse
import kr.hanchae.moyeotrip.controller.feed.response.FeedTripResponse
import kr.hanchae.moyeotrip.entity.feed.Feed
import kr.hanchae.moyeotrip.entity.feed.FeedComment
import kr.hanchae.moyeotrip.entity.feed.FeedLike
import kr.hanchae.moyeotrip.entity.feed.FeedVisibility
import kr.hanchae.moyeotrip.entity.user.User
import kr.hanchae.moyeotrip.exception.BaseException
import kr.hanchae.moyeotrip.exception.ErrorCode
import kr.hanchae.moyeotrip.exception.UserNotFoundException
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
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.time.LocalDate

@Service
class FeedService(
    private val feedRepository: FeedRepository,
    private val feedLikeRepository: FeedLikeRepository,
    private val feedCommentRepository: FeedCommentRepository,
    private val chatRoomRepository: ChatRoomRepository,
    private val participantRepository: ChatRoomParticipantRepository,
    private val userRepository: UserRepository,
    private val userBlockRepository: UserBlockRepository,
    private val friendshipRepository: FriendshipRepository,
    private val objectStorageRepository: ObjectStorageRepository,
    private val notificationService: NotificationService,
) {
    @Transactional
    fun createFeed(
        userId: Long,
        request: CreateFeedRequest,
        images: List<MultipartFile>,
    ): FeedResponse {
        validateImages(images)
        val user = findUser(userId)
        val room = chatRoomRepository.findById(request.chatRoomId).orElseThrow { BaseException(ErrorCode.CHAT_ROOM_NOT_FOUND) }
        if (!participantRepository.hasCompletedTrip(room.id, userId, LocalDate.now())) {
            throw BaseException(ErrorCode.FORBIDDEN)
        }
        if (feedRepository.existsByChatRoomIdAndAuthorId(room.id, userId)) {
            throw BaseException(ErrorCode.BAD_REQUEST, "같은 여행에는 피드를 한 번만 작성할 수 있습니다.")
        }

        val uploadedKeys = mutableListOf<String>()
        try {
            images.forEach { uploadedKeys += uploadImage(it) }
            val feed =
                Feed(
                    author = user,
                    chatRoom = room,
                    content = request.content.trim(),
                    visibility = request.visibility,
                )
            uploadedKeys.forEachIndexed { index, fileName -> feed.addImage(fileName, index) }
            return feedRepository.save(feed).toResponse(userId)
        } catch (exception: RuntimeException) {
            uploadedKeys.forEach { runCatching { objectStorageRepository.delete(it) } }
            throw exception
        }
    }

    @Transactional(readOnly = true)
    fun getFeeds(
        userId: Long,
        tab: FeedTab,
        beforeFeedId: Long?,
        limit: Int,
    ): FeedPageResponse {
        val pageSize = limit.coerceIn(1, MAX_PAGE_SIZE)
        if (tab == FeedTab.DISCOVER) {
            return FeedPageResponse(
                feeds = feedRepository.findRandomDiscoverFeeds(userId, pageSize).map { it.toResponse(userId) },
                nextId = null,
            )
        }
        val pageable = PageRequest.of(0, pageSize + 1)
        val beforeId = beforeFeedId ?: Long.MAX_VALUE
        val fetched =
            feedRepository.findFriendFeeds(
                userId,
                beforeId,
                FeedVisibility.PUBLIC,
                FeedVisibility.FRIENDS,
                pageable,
            )
        val hasNext = fetched.size > pageSize
        val feeds = fetched.take(pageSize)
        return FeedPageResponse(
            feeds = feeds.map { it.toResponse(userId) },
            nextId = feeds.lastOrNull()?.id?.takeIf { hasNext },
        )
    }

    @Transactional(readOnly = true)
    fun getFeed(
        userId: Long,
        feedId: Long,
    ): FeedResponse = requireVisibleFeed(userId, feedId).toResponse(userId)

    @Transactional
    fun toggleLike(
        userId: Long,
        feedId: Long,
    ): FeedLikeResponse {
        val feed = requireVisibleFeed(userId, feedId)
        val likeCount = feedLikeRepository.countByFeedId(feedId)
        val existing = feedLikeRepository.findByFeedIdAndUserId(feedId, userId)
        if (existing != null) {
            feedLikeRepository.delete(existing)
            return FeedLikeResponse(liked = false, likeCount = (likeCount - 1L).coerceAtLeast(0L))
        }
        val user = findUser(userId)
        feedLikeRepository.save(FeedLike(feed = feed, user = user))
        notificationService.notifyFeedLiked(feed, user)
        return FeedLikeResponse(liked = true, likeCount = likeCount + 1L)
    }

    @Transactional(readOnly = true)
    fun getComments(
        userId: Long,
        feedId: Long,
    ): List<FeedCommentResponse> {
        requireVisibleFeed(userId, feedId)
        return feedCommentRepository
            .findAllByFeedIdAndParentIsNullOrderByCreatedDateTimeAsc(feedId)
            .map { comment ->
                comment.toResponse(
                    replies = feedCommentRepository.findAllByParentIdOrderByCreatedDateTimeAsc(comment.id).map { it.toResponse() },
                )
            }
    }

    @Transactional
    fun createComment(
        userId: Long,
        feedId: Long,
        request: CreateFeedCommentRequest,
    ): FeedCommentResponse {
        val feed = requireVisibleFeed(userId, feedId)
        val parent =
            request.parentCommentId?.let {
                feedCommentRepository.findByIdAndFeedId(it, feedId)?.takeIf { comment -> comment.parent == null }
                    ?: throw BaseException(ErrorCode.RESOURCE_NOT_FOUND)
            }
        return feedCommentRepository
            .save(FeedComment(feed = feed, author = findUser(userId), parent = parent, content = request.content.trim()))
            .toResponse()
    }

    private fun Feed.toResponse(userId: Long): FeedResponse {
        val information = checkNotNull(author.information)
        return FeedResponse(
            feedId = id,
            author =
                FeedAuthorResponse(
                    userId = author.id,
                    nickname = information.nickname,
                    profileImageUrl = information.profileFileName?.let(objectStorageRepository::getDownloadUrl),
                ),
            content = content,
            visibility = visibility,
            images = images.map { FeedImageResponse(it.id, objectStorageRepository.getDownloadUrl(it.fileName), it.sequence) },
            trip =
                FeedTripResponse(
                    chatRoomId = chatRoom.id,
                    courseId = chatRoom.course.id,
                    courseTitle = chatRoom.course.title,
                    startDate = chatRoom.startDate,
                    endDate = chatRoom.endDate,
                    places =
                        chatRoom.course.places.map {
                            FeedPlaceResponse(
                                tourismContentId = it.tourismContent.contentId,
                                title = it.tourismContent.title,
                                latitude = it.tourismContent.latitude,
                                longitude = it.tourismContent.longitude,
                                dayNumber = it.dayNumber,
                                sequence = it.sequence,
                                visitTime = it.visitTime,
                            )
                        },
                ),
            likeCount = feedLikeRepository.countByFeedId(id),
            commentCount = feedCommentRepository.countByFeedId(id),
            liked = feedLikeRepository.existsByFeedIdAndUserId(id, userId),
            createdAt = createdDateTime,
        )
    }

    private fun FeedComment.toResponse(replies: List<FeedCommentResponse> = emptyList()): FeedCommentResponse {
        val information = checkNotNull(author.information)
        return FeedCommentResponse(
            commentId = id,
            author =
                FeedAuthorResponse(
                    userId = author.id,
                    nickname = information.nickname,
                    profileImageUrl = information.profileFileName?.let(objectStorageRepository::getDownloadUrl),
                ),
            content = content,
            createdAt = createdDateTime,
            replies = replies,
        )
    }

    private fun validateImages(images: List<MultipartFile>) {
        if (images.isEmpty() || images.size > MAX_IMAGE_COUNT) throw BaseException(ErrorCode.BAD_REQUEST)
        images.forEach {
            if (it.isEmpty || it.size > MAX_IMAGE_SIZE || it.contentType?.startsWith("image/") != true) {
                throw BaseException(ErrorCode.BAD_REQUEST)
            }
        }
    }

    private fun uploadImage(image: MultipartFile): String =
        objectStorageRepository.upload(
            FEED_IMAGE_PATH,
            ObjectStorageRepository.generateFileName(fileExtension(image)),
            image.inputStream,
        )

    private fun fileExtension(image: MultipartFile): String =
        image.originalFilename
            ?.substringAfterLast('.', "jpg")
            ?.lowercase()
            ?.takeIf { it in ALLOWED_EXTENSIONS }
            ?: "jpg"

    private fun findFeed(feedId: Long): Feed = feedRepository.findById(feedId).orElseThrow { BaseException(ErrorCode.RESOURCE_NOT_FOUND) }

    private fun requireVisibleFeed(
        userId: Long,
        feedId: Long,
    ): Feed {
        val feed = findFeed(feedId)
        if (userBlockRepository.existsBetween(userId, feed.author.id)) throw BaseException(ErrorCode.FORBIDDEN)
        if (!feed.isVisibleTo(userId)) throw BaseException(ErrorCode.FORBIDDEN)
        return feed
    }

    private fun Feed.isVisibleTo(userId: Long): Boolean =
        when (visibility) {
            FeedVisibility.PUBLIC -> true
            FeedVisibility.PRIVATE -> author.id == userId
            FeedVisibility.FRIENDS ->
                author.id == userId || friendshipRepository.existsBetween(userId, author.id)
        }

    private fun findUser(userId: Long): User = userRepository.findById(userId).orElseThrow { UserNotFoundException(userId) }

    companion object {
        private const val MAX_PAGE_SIZE = 50
        private const val MAX_IMAGE_COUNT = 10
        private const val MAX_IMAGE_SIZE = 20L * 1024L * 1024L
        private const val FEED_IMAGE_PATH = "feed/image/"
        private val ALLOWED_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp", "heic")
    }
}
