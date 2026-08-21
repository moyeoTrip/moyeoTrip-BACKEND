package kr.hanchae.moyeotrip.service.user

import kr.hanchae.moyeotrip.controller.user.response.FollowListResponse
import kr.hanchae.moyeotrip.controller.user.response.FollowResponse
import kr.hanchae.moyeotrip.controller.user.response.FollowUserResponse
import kr.hanchae.moyeotrip.entity.user.User
import kr.hanchae.moyeotrip.entity.user.UserFollow
import kr.hanchae.moyeotrip.exception.BaseException
import kr.hanchae.moyeotrip.exception.ErrorCode
import kr.hanchae.moyeotrip.exception.UserNotFoundException
import kr.hanchae.moyeotrip.repository.ObjectStorageRepository
import kr.hanchae.moyeotrip.repository.UserFollowRepository
import kr.hanchae.moyeotrip.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class FollowService(
    private val userRepository: UserRepository,
    private val followRepository: UserFollowRepository,
    private val objectStorageRepository: ObjectStorageRepository,
) {
    @Transactional
    fun toggleFollow(
        followerId: Long,
        followingId: Long,
    ): FollowResponse {
        if (followerId == followingId) throw BaseException(ErrorCode.BAD_REQUEST)
        val existingFollow = followRepository.findByFollowerIdAndFollowingId(followerId, followingId)
        if (existingFollow != null) {
            followRepository.delete(existingFollow)
            return FollowResponse(userId = followingId, following = false)
        }
        val follower = findUser(followerId)
        val following = findUser(followingId)
        followRepository.save(UserFollow(follower = follower, following = following))
        return FollowResponse(userId = followingId, following = true)
    }

    @Transactional(readOnly = true)
    fun getFollowers(userId: Long): FollowListResponse {
        findUser(userId)
        val follows = followRepository.findAllByFollowingIdOrderByCreatedDateTimeDesc(userId)
        return FollowListResponse(
            totalCount = followRepository.countByFollowingId(userId),
            users = follows.map { it.follower.toResponse() },
        )
    }

    @Transactional(readOnly = true)
    fun getFollowing(userId: Long): FollowListResponse {
        findUser(userId)
        val follows = followRepository.findAllByFollowerIdOrderByCreatedDateTimeDesc(userId)
        return FollowListResponse(
            totalCount = followRepository.countByFollowerId(userId),
            users = follows.map { it.following.toResponse() },
        )
    }

    private fun findUser(userId: Long): User = userRepository.findById(userId).orElseThrow { UserNotFoundException(userId) }

    private fun User.toResponse(): FollowUserResponse {
        val information = checkNotNull(information)
        return FollowUserResponse(
            userId = id,
            nickname = information.nickname,
            profileImageUrl = information.profileFileName?.let(objectStorageRepository::getDownloadUrl),
            introduction = information.introduction,
        )
    }
}
