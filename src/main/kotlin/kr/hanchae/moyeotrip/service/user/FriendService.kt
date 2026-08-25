package kr.hanchae.moyeotrip.service.user

import kr.hanchae.moyeotrip.controller.user.response.FriendListResponse
import kr.hanchae.moyeotrip.controller.user.response.FriendRequestListResponse
import kr.hanchae.moyeotrip.controller.user.response.FriendRequestResponse
import kr.hanchae.moyeotrip.controller.user.response.FriendResponse
import kr.hanchae.moyeotrip.controller.user.response.FriendUserResponse
import kr.hanchae.moyeotrip.entity.user.FriendRequest
import kr.hanchae.moyeotrip.entity.user.Friendship
import kr.hanchae.moyeotrip.entity.user.User
import kr.hanchae.moyeotrip.exception.BaseException
import kr.hanchae.moyeotrip.exception.ErrorCode
import kr.hanchae.moyeotrip.exception.UserNotFoundException
import kr.hanchae.moyeotrip.repository.FriendRequestRepository
import kr.hanchae.moyeotrip.repository.FriendshipRepository
import kr.hanchae.moyeotrip.repository.ObjectStorageRepository
import kr.hanchae.moyeotrip.repository.UserBlockRepository
import kr.hanchae.moyeotrip.repository.UserRepository
import kr.hanchae.moyeotrip.service.notification.NotificationService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.LocalDateTime

@Service
class FriendService(
    private val userRepository: UserRepository,
    private val friendshipRepository: FriendshipRepository,
    private val friendRequestRepository: FriendRequestRepository,
    private val blockRepository: UserBlockRepository,
    private val objectStorageRepository: ObjectStorageRepository,
    private val notificationService: NotificationService,
) {
    @Transactional
    fun sendRequest(
        requesterId: Long,
        receiverId: Long,
    ): FriendRequestResponse {
        validateRelationship(requesterId, receiverId)
        if (friendshipRepository.existsBetween(requesterId, receiverId)) {
            throw BaseException(ErrorCode.ALREADY_FRIEND)
        }
        friendRequestRepository.findByRequesterIdAndReceiverId(receiverId, requesterId)?.let {
            throw BaseException(ErrorCode.REVERSE_FRIEND_REQUEST_EXISTS)
        }
        friendRequestRepository.findByRequesterIdAndReceiverId(requesterId, receiverId)?.let { return it.toSentResponse() }

        val request =
            friendRequestRepository.save(
                FriendRequest(
                    requester = findUser(requesterId),
                    receiver = findUser(receiverId),
                ),
            )
        notificationService.notifyFriendRequested(request)
        return request.toSentResponse()
    }

    @Transactional
    fun acceptRequest(
        receiverId: Long,
        requestId: Long,
    ): FriendResponse {
        val request = findReceivedRequest(receiverId, requestId)
        if (blockRepository.existsBetween(request.requester.id, receiverId)) throw BaseException(ErrorCode.USER_BLOCK_RELATIONSHIP)
        val friendship =
            friendshipRepository.findBetween(request.requester.id, receiverId)
                ?: friendshipRepository.save(createFriendship(request.requester, request.receiver))
        friendRequestRepository.deleteBetween(request.requester.id, receiverId)
        notificationService.notifyFriendAccepted(friendship, acceptedBy = request.receiver)
        return friendship.toResponse(receiverId)
    }

    @Transactional
    fun rejectRequest(
        receiverId: Long,
        requestId: Long,
    ) {
        friendRequestRepository.delete(findReceivedRequest(receiverId, requestId))
    }

    @Transactional
    fun cancelRequest(
        requesterId: Long,
        requestId: Long,
    ) {
        val request =
            friendRequestRepository.findByIdAndRequesterId(requestId, requesterId)
                ?: throw BaseException(ErrorCode.FRIEND_REQUEST_NOT_FOUND)
        friendRequestRepository.delete(request)
    }

    @Transactional(readOnly = true)
    fun getReceivedRequests(userId: Long): FriendRequestListResponse {
        val requests = friendRequestRepository.findAllByReceiverIdOrderByCreatedDateTimeDesc(userId)
        return FriendRequestListResponse(requests.size, requests.map { it.toReceivedResponse() })
    }

    @Transactional(readOnly = true)
    fun getSentRequests(userId: Long): FriendRequestListResponse {
        val requests = friendRequestRepository.findAllByRequesterIdOrderByCreatedDateTimeDesc(userId)
        return FriendRequestListResponse(requests.size, requests.map { it.toSentResponse() })
    }

    @Transactional(readOnly = true)
    fun getFriends(userId: Long): FriendListResponse {
        val friendships = friendshipRepository.findAllByUserId(userId)
        return FriendListResponse(friendships.size, friendships.map { it.toResponse(userId) })
    }

    @Transactional
    fun deleteFriend(
        userId: Long,
        friendId: Long,
    ) {
        if (friendshipRepository.deleteBetween(userId, friendId) == 0) throw BaseException(ErrorCode.FRIENDSHIP_NOT_FOUND)
    }

    private fun validateRelationship(
        firstUserId: Long,
        secondUserId: Long,
    ) {
        if (firstUserId == secondUserId) throw BaseException(ErrorCode.SELF_FRIEND_REQUEST_NOT_ALLOWED)
        if (blockRepository.existsBetween(firstUserId, secondUserId)) throw BaseException(ErrorCode.USER_BLOCK_RELATIONSHIP)
    }

    private fun findReceivedRequest(
        receiverId: Long,
        requestId: Long,
    ): FriendRequest =
        friendRequestRepository.findByIdAndReceiverId(requestId, receiverId)
            ?: throw BaseException(ErrorCode.FRIEND_REQUEST_NOT_FOUND)

    private fun findUser(userId: Long): User = userRepository.findById(userId).orElseThrow { UserNotFoundException(userId) }

    private fun createFriendship(
        first: User,
        second: User,
    ): Friendship =
        if (first.id < second.id) {
            Friendship(firstUser = first, secondUser = second)
        } else {
            Friendship(firstUser = second, secondUser = first)
        }

    private fun FriendRequest.toReceivedResponse(): FriendRequestResponse =
        FriendRequestResponse(id, requester.toResponse(), createdDateTime)

    private fun FriendRequest.toSentResponse(): FriendRequestResponse = FriendRequestResponse(id, receiver.toResponse(), createdDateTime)

    private fun Friendship.toResponse(userId: Long): FriendResponse {
        val friend = friendOf(userId)
        return FriendResponse(id, friend.toResponse(), friend.lastLoginDateTime?.toRelativeTime())
    }

    private fun User.toResponse(): FriendUserResponse {
        val information = checkNotNull(information)
        return FriendUserResponse(
            userId = id,
            nickname = information.nickname,
            profileImageUrl = information.profileFileName?.let(objectStorageRepository::getDownloadUrl),
            introduction = information.introduction,
        )
    }

    private fun LocalDateTime.toRelativeTime(now: LocalDateTime = LocalDateTime.now()): String {
        val elapsedMinutes = Duration.between(this, now).toMinutes().coerceAtLeast(0)
        return when {
            elapsedMinutes < 1 -> "방금 전"
            elapsedMinutes < MINUTES_PER_HOUR -> "${elapsedMinutes}분 전"
            elapsedMinutes < MINUTES_PER_DAY -> "${elapsedMinutes / MINUTES_PER_HOUR}시간 전"
            else -> "${elapsedMinutes / MINUTES_PER_DAY}일 전"
        }
    }

    companion object {
        private const val MINUTES_PER_HOUR = 60L
        private const val MINUTES_PER_DAY = MINUTES_PER_HOUR * 24
    }
}
