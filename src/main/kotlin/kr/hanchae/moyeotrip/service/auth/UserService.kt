package kr.hanchae.moyeotrip.service.auth

import kr.hanchae.moyeotrip.client.ProfileImageGenerationClient
import kr.hanchae.moyeotrip.client.ProfileImagePromptFactory
import kr.hanchae.moyeotrip.controller.user.response.ProfileImageCandidateResponse
import kr.hanchae.moyeotrip.controller.user.response.ProfileImageCandidatesResponse
import kr.hanchae.moyeotrip.controller.user.response.ProfileImageGenerationResponse
import kr.hanchae.moyeotrip.controller.user.response.ProfileImageSelectionResponse
import kr.hanchae.moyeotrip.entity.user.User
import kr.hanchae.moyeotrip.entity.user.UserProfileImage
import kr.hanchae.moyeotrip.exception.BaseException
import kr.hanchae.moyeotrip.exception.ErrorCode
import kr.hanchae.moyeotrip.exception.UserNotFoundException
import kr.hanchae.moyeotrip.repository.ObjectStorageRepository
import kr.hanchae.moyeotrip.repository.UserProfileImageRepository
import kr.hanchae.moyeotrip.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager

@Service
class UserService(
    private val userRepository: UserRepository,
    private val objectStorageRepository: ObjectStorageRepository,
    private val profileImageGenerationClient: ProfileImageGenerationClient,
    private val profileImagePromptFactory: ProfileImagePromptFactory,
    private val userProfileImageRepository: UserProfileImageRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun generateProfileImage(userId: Long): ProfileImageGenerationResponse {
        val user = userRepository.findByIdForUpdate(userId) ?: throw UserNotFoundException(userId)
        requireProfileSetupStarted(user)
        if (!user.canGenerateProfileImage()) {
            throw BaseException(
                ErrorCode.PROFILE_IMAGE_GENERATION_LIMIT,
                ErrorCode.PROFILE_IMAGE_GENERATION_LIMIT.errorMessage,
            )
        }

        val information = checkNotNull(user.information)
        val prompt = profileImagePromptFactory.create(information.nickname, information.nicknameColor)
        val imageBytes = profileImageGenerationClient.generate(prompt)
        val generatedImageKey = objectStorageRepository.uploadGeneratedProfileImage(imageBytes)
        scheduleGeneratedImageCleanupOnRollback(generatedImageKey)
        val profileImage =
            userProfileImageRepository.save(
                UserProfileImage(user = user, fileName = generatedImageKey),
            )
        user.recordProfileImageGeneration()

        return ProfileImageGenerationResponse(
            candidate = profileImage.toResponse(selectedFileName = information.profileFileName),
            generationCount = user.profileImageGenerationCount,
            remainingGenerationCount = user.remainingProfileImageGenerationCount(),
            signupState = user.signupState,
        )
    }

    @Transactional(readOnly = true)
    fun getProfileImages(userId: Long): ProfileImageCandidatesResponse {
        val user = userRepository.findById(userId).orElseThrow { UserNotFoundException(userId) }
        requireProfileSetupStarted(user)
        val selectedFileName = checkNotNull(user.information).profileFileName
        return ProfileImageCandidatesResponse(
            candidates =
                userProfileImageRepository
                    .findAllByUserIdOrderByCreatedDateTimeAsc(userId)
                    .map { it.toResponse(selectedFileName) },
            generationCount = user.profileImageGenerationCount,
            remainingGenerationCount = user.remainingProfileImageGenerationCount(),
            signupState = user.signupState,
        )
    }

    @Transactional
    fun selectProfileImage(
        userId: Long,
        profileImageId: Long,
    ): ProfileImageSelectionResponse {
        val user = userRepository.findByIdForUpdate(userId) ?: throw UserNotFoundException(userId)
        requireProfileSetupStarted(user)
        val profileImage =
            userProfileImageRepository.findByIdAndUserId(profileImageId, userId)
                ?: throw BaseException(
                    ErrorCode.PROFILE_IMAGE_NOT_FOUND,
                    ErrorCode.PROFILE_IMAGE_NOT_FOUND.errorMessage,
                )
        user.selectProfileImage(profileImage.fileName)
        return ProfileImageSelectionResponse(
            selectedImage = profileImage.toResponse(selectedFileName = profileImage.fileName),
            signupState = user.signupState,
        )
    }

    private fun requireProfileSetupStarted(user: User) {
        if (user.information == null) {
            throw BaseException(ErrorCode.USER_INFO_REQUIRED, ErrorCode.USER_INFO_REQUIRED.errorMessage)
        }
    }

    private fun UserProfileImage.toResponse(selectedFileName: String?): ProfileImageCandidateResponse =
        ProfileImageCandidateResponse(
            profileImageId = id,
            profileImageUrl = objectStorageRepository.getDownloadUrl(fileName),
            selected = fileName == selectedFileName,
        )

    private fun scheduleGeneratedImageCleanupOnRollback(generatedImageKey: String) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return
        }
        TransactionSynchronizationManager.registerSynchronization(
            object : TransactionSynchronization {
                override fun afterCompletion(status: Int) {
                    if (status != TransactionSynchronization.STATUS_COMMITTED) {
                        deleteQuietly(generatedImageKey)
                    }
                }
            },
        )
    }

    private fun deleteQuietly(key: String) {
        try {
            objectStorageRepository.delete(key)
        } catch (exception: Exception) {
            log.warn("프로필 이미지 객체 정리에 실패했습니다. key={}", key, exception)
        }
    }
}
