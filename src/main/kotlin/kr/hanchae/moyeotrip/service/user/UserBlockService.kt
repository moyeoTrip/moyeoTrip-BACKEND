package kr.hanchae.moyeotrip.service.user

import kr.hanchae.moyeotrip.controller.user.response.BlockedUserResponse
import kr.hanchae.moyeotrip.controller.user.response.UserBlockResponse
import kr.hanchae.moyeotrip.entity.user.UserBlock
import kr.hanchae.moyeotrip.exception.BaseException
import kr.hanchae.moyeotrip.exception.ErrorCode
import kr.hanchae.moyeotrip.exception.UserNotFoundException
import kr.hanchae.moyeotrip.repository.FriendRequestRepository
import kr.hanchae.moyeotrip.repository.FriendshipRepository
import kr.hanchae.moyeotrip.repository.ObjectStorageRepository
import kr.hanchae.moyeotrip.repository.UserBlockRepository
import kr.hanchae.moyeotrip.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserBlockService(
    private val userRepository: UserRepository,
    private val blockRepository: UserBlockRepository,
    private val friendshipRepository: FriendshipRepository,
    private val friendRequestRepository: FriendRequestRepository,
    private val objectStorageRepository: ObjectStorageRepository,
) {
    @Transactional
    fun block(
        blockerId: Long,
        blockedId: Long,
    ): UserBlockResponse {
        if (blockerId == blockedId) throw BaseException(ErrorCode.SELF_BLOCK_NOT_ALLOWED)
        val blocker = userRepository.findById(blockerId).orElseThrow { UserNotFoundException(blockerId) }
        val blocked = userRepository.findById(blockedId).orElseThrow { UserNotFoundException(blockedId) }
        if (blockRepository.findByBlockerIdAndBlockedId(blockerId, blockedId) == null) {
            blockRepository.save(UserBlock(blocker = blocker, blocked = blocked))
        }
        friendshipRepository.deleteBetween(blockerId, blockedId)
        friendRequestRepository.deleteBetween(blockerId, blockedId)
        return UserBlockResponse(userId = blockedId, blocked = true)
    }

    @Transactional
    fun unblock(
        blockerId: Long,
        blockedId: Long,
    ): UserBlockResponse {
        blockRepository.findByBlockerIdAndBlockedId(blockerId, blockedId)?.let(blockRepository::delete)
        return UserBlockResponse(userId = blockedId, blocked = false)
    }

    @Transactional(readOnly = true)
    fun getBlockedUsers(userId: Long): List<BlockedUserResponse> =
        blockRepository.findAllByBlockerIdOrderByCreatedDateTimeDesc(userId).map {
            val information = checkNotNull(it.blocked.information)
            BlockedUserResponse(
                userId = it.blocked.id,
                nickname = information.nickname,
                profileImageUrl = information.profileFileName?.let(objectStorageRepository::getDownloadUrl),
                blockedAt = it.createdDateTime,
            )
        }
}
