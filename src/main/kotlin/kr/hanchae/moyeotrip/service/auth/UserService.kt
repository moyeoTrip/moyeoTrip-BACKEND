package kr.hanchae.moyeotrip.service.auth

import kr.hanchae.moyeotrip.repository.ObjectStorageRepository
import kr.hanchae.moyeotrip.repository.UserRepository
import org.springframework.stereotype.Service

@Service
class UserService(
    private val userRepository: UserRepository,
    private val objectStorageRepository: ObjectStorageRepository,
) {
    /*
    @Transactional(readOnly = true)
    fun getUserInfo(userId: Long): UserInfoResponse {
        val user = userRepository.findByIdOrNull(userId) ?: throw UserNotFoundException(userId)
        return UserInfoResponse(
            user.information.nickname,
            user.information.profileFileName?.let { objectStorageRepository.getDownloadUrl(it) },
        )
    }

    @RedisLock(
        prefix = "userNickname",
        key = "#nicknameUpdateRequest.nickname",
        waitTime = 5,
        leaseTime = 3,
    )
    @Transactional
    fun updateNickname(
        nicknameUpdateRequest: NicknameUpdateRequest,
        userId: Long,
    ) {
        if (userRepository.existsByNicknameAndIdNot(nicknameUpdateRequest.nickname, userId)) {
            throw AlreadyExistNicknameException()
        }
        val user = userRepository.findByIdOrNull(userId) ?: throw UserNotFoundException(userId)
        user.updateNickname(nicknameUpdateRequest.nickname)
    }

    @Transactional
    fun updateProfileImage(
        userId: Long,
        profileImage: MultipartFile,
    ) {
        val user = userRepository.findByIdOrNull(userId) ?: throw UserNotFoundException(userId)
        val prevProfileImage = user.profileImage
        val profileImageUrl =
            objectStorageRepository.upload(ObjectStorageRepository.USER_PROFILE_IMAGE_PATH, profileImage)
        prevProfileImage?.let { objectStorageRepository.delete(it) }
        user.profileImage = profileImageUrl
    }

    @Transactional
    fun deleteUser(userId: Long) {
        val user = userRepository.findByIdOrNull(userId) ?: throw UserNotFoundException(userId)
        user.profileImage?.let { objectStorageRepository.delete(it) }
        userOauthInfoRepository.deleteByUser(user)
        val completionImages = courseCompletionImageRepository.findByUser(user)
        completionImages.forEach { objectStorageRepository.delete(it.image) }
        courseCompletionImageRepository.deleteAll(completionImages)
        courseLikeHistoryRepository.deleteByUser(user)
        courseCompletionHistoryRepository.deleteByUserId(user.id)
        termsAgreeHistoryRepository.deleteByUserId(user.id)
        userRepository.delete(user)
    }*/
}
