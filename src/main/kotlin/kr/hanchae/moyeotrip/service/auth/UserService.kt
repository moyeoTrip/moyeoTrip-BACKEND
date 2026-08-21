package kr.hanchae.moyeotrip.service.auth

import kr.hanchae.moyeotrip.client.ProfileImageGenerationClient
import kr.hanchae.moyeotrip.client.ProfileImagePromptFactory
import kr.hanchae.moyeotrip.controller.user.request.UpdateProfileRequest
import kr.hanchae.moyeotrip.controller.user.response.InterestedRegionResponse
import kr.hanchae.moyeotrip.controller.user.response.MyProfileResponse
import kr.hanchae.moyeotrip.controller.user.response.ProfileImageCandidateResponse
import kr.hanchae.moyeotrip.controller.user.response.ProfileImageCandidatesResponse
import kr.hanchae.moyeotrip.controller.user.response.ProfileImageGenerationResponse
import kr.hanchae.moyeotrip.controller.user.response.ProfileImageSelectionResponse
import kr.hanchae.moyeotrip.controller.user.response.ProfileOptionResponse
import kr.hanchae.moyeotrip.controller.user.response.ProfileOptionsResponse
import kr.hanchae.moyeotrip.entity.user.TravelStyle
import kr.hanchae.moyeotrip.entity.user.User
import kr.hanchae.moyeotrip.entity.user.UserProfileImage
import kr.hanchae.moyeotrip.exception.BaseException
import kr.hanchae.moyeotrip.exception.ErrorCode
import kr.hanchae.moyeotrip.exception.UserNotFoundException
import kr.hanchae.moyeotrip.repository.LegalDongCodeRepository
import kr.hanchae.moyeotrip.repository.ObjectStorageRepository
import kr.hanchae.moyeotrip.repository.UserProfileImageRepository
import kr.hanchae.moyeotrip.repository.UserRepository
import kr.hanchae.moyeotrip.utils.jwt.JwtUtil
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.time.LocalDate
import java.time.Period

@Service
class UserService(
    private val userRepository: UserRepository,
    private val objectStorageRepository: ObjectStorageRepository,
    private val profileImageGenerationClient: ProfileImageGenerationClient,
    private val profileImagePromptFactory: ProfileImagePromptFactory,
    private val userProfileImageRepository: UserProfileImageRepository,
    private val legalDongCodeRepository: LegalDongCodeRepository,
    private val jwtUtil: JwtUtil,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional(readOnly = true)
    fun getProfile(userId: Long): MyProfileResponse {
        val user = userRepository.findById(userId).orElseThrow { UserNotFoundException(userId) }
        requireProfileSetupStarted(user)
        return user.toProfileResponse()
    }

    @Transactional
    fun updateProfile(
        userId: Long,
        request: UpdateProfileRequest,
    ): MyProfileResponse {
        val user = userRepository.findByIdForUpdate(userId) ?: throw UserNotFoundException(userId)
        requireProfileSetupStarted(user)
        if (Period.between(request.birthDate, LocalDate.now()).years < MINIMUM_PROFILE_AGE) {
            throw BaseException(ErrorCode.MINIMUM_SIGNUP_AGE_NOT_MET)
        }
        val interestedRegions =
            legalDongCodeRepository.findAllByRegionCodeAndSignguCodeIn(
                GYEONGSANGBUKDO_REGION_CODE,
                request.interestedRegionCodes,
            )
        if (interestedRegions.map { it.signguCode }.toSet() != request.interestedRegionCodes) {
            throw BaseException(ErrorCode.BAD_REQUEST)
        }
        user.updateProfile(
            introduction = request.introduction?.trim()?.takeIf(String::isNotEmpty),
            travelStyles = request.travelStyles,
            interestedRegions = interestedRegions.toSet(),
            birthDate = request.birthDate,
            gender = request.gender,
        )
        return user.toProfileResponse()
    }

    @Transactional(readOnly = true)
    fun getProfileOptions(): ProfileOptionsResponse =
        ProfileOptionsResponse(
            travelStyles = TravelStyle.entries.map { ProfileOptionResponse(it, it.label) },
            interestedRegions =
                legalDongCodeRepository
                    .findAllByRegionCodeOrderBySignguNameAsc(GYEONGSANGBUKDO_REGION_CODE)
                    .map { InterestedRegionResponse(it.signguCode, it.signguName) },
        )

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

    @Transactional
    fun withdraw(userId: Long) {
        val user = userRepository.findByIdForUpdate(userId) ?: throw UserNotFoundException(userId)
        val profileImageKeys =
            userProfileImageRepository
                .findFileNamesByUserIdOrderByCreatedDateTimeAsc(userId)

        userRepository.delete(user)
        scheduleWithdrawalCleanupAfterCommit(userId, profileImageKeys)
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

    private fun User.toProfileResponse(): MyProfileResponse {
        val information = checkNotNull(information)
        return MyProfileResponse(
            nickname = information.nickname,
            profileImageUrl = information.profileFileName?.let(objectStorageRepository::getDownloadUrl),
            introduction = information.introduction,
            travelStyles = travelStyles,
            interestedRegions = interestedRegions.sortedBy { it.signguName }.map { InterestedRegionResponse(it.signguCode, it.signguName) },
            birthDate = information.birthDate,
            gender = information.gender,
        )
    }

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

    private fun scheduleWithdrawalCleanupAfterCommit(
        userId: Long,
        profileImageKeys: List<String>,
    ) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            cleanupWithdrawalResources(userId, profileImageKeys)
            return
        }
        TransactionSynchronizationManager.registerSynchronization(
            object : TransactionSynchronization {
                override fun afterCommit() {
                    cleanupWithdrawalResources(userId, profileImageKeys)
                }
            },
        )
    }

    private fun cleanupWithdrawalResources(
        userId: Long,
        profileImageKeys: List<String>,
    ) {
        profileImageKeys.forEach(::deleteQuietly)
        try {
            jwtUtil.deleteCachedRefreshTokenRotateId(userId)
        } catch (exception: Exception) {
            log.warn("탈퇴 사용자의 Refresh Token 캐시 정리에 실패했습니다. userId={}", userId, exception)
        }
    }

    private fun deleteQuietly(key: String) {
        try {
            objectStorageRepository.delete(key)
        } catch (exception: Exception) {
            log.warn("프로필 이미지 객체 정리에 실패했습니다. key={}", key, exception)
        }
    }

    companion object {
        private const val MINIMUM_PROFILE_AGE = 20
        private const val GYEONGSANGBUKDO_REGION_CODE = "47"
    }
}
