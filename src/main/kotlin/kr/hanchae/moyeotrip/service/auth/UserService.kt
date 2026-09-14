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
import kr.hanchae.moyeotrip.controller.user.response.ProfileOptionsResponse
import kr.hanchae.moyeotrip.controller.user.response.PublicProfileResponse
import kr.hanchae.moyeotrip.controller.user.response.TravelStyleResponse
import kr.hanchae.moyeotrip.controller.user.response.WithdrawalReasonResponse
import kr.hanchae.moyeotrip.entity.feed.FeedVisibility
import kr.hanchae.moyeotrip.entity.notification.ChatNotificationMode
import kr.hanchae.moyeotrip.entity.user.AgePolicy
import kr.hanchae.moyeotrip.entity.user.SignupState
import kr.hanchae.moyeotrip.entity.user.User
import kr.hanchae.moyeotrip.entity.user.UserProfileImage
import kr.hanchae.moyeotrip.entity.user.WithdrawalReason
import kr.hanchae.moyeotrip.entity.user.WithdrawalReasonRecord
import kr.hanchae.moyeotrip.exception.BaseException
import kr.hanchae.moyeotrip.exception.ErrorCode
import kr.hanchae.moyeotrip.exception.UserNotFoundException
import kr.hanchae.moyeotrip.repository.ChatRoomParticipantRepository
import kr.hanchae.moyeotrip.repository.FeedRepository
import kr.hanchae.moyeotrip.repository.LegalDongCodeRepository
import kr.hanchae.moyeotrip.repository.NotificationSettingRepository
import kr.hanchae.moyeotrip.repository.ObjectStorageRepository
import kr.hanchae.moyeotrip.repository.TravelStyleRepository
import kr.hanchae.moyeotrip.repository.UserProfileImageRepository
import kr.hanchae.moyeotrip.repository.UserRepository
import kr.hanchae.moyeotrip.repository.UserWithdrawalDataRepository
import kr.hanchae.moyeotrip.repository.WithdrawalReasonRepository
import kr.hanchae.moyeotrip.utils.FhdWebpImageOptimizer
import kr.hanchae.moyeotrip.utils.jwt.JwtUtil
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Period

@Service
class UserService(
    private val userRepository: UserRepository,
    private val objectStorageRepository: ObjectStorageRepository,
    private val profileImageGenerationClient: ProfileImageGenerationClient,
    private val profileImagePromptFactory: ProfileImagePromptFactory,
    private val userProfileImageRepository: UserProfileImageRepository,
    private val legalDongCodeRepository: LegalDongCodeRepository,
    private val travelStyleRepository: TravelStyleRepository,
    private val notificationSettingRepository: NotificationSettingRepository,
    private val fhdWebpImageOptimizer: FhdWebpImageOptimizer,
    private val jwtUtil: JwtUtil,
    private val userWithdrawalDataRepository: UserWithdrawalDataRepository,
    private val chatRoomParticipantRepository: ChatRoomParticipantRepository,
    private val feedRepository: FeedRepository,
    private val withdrawalReasonRepository: WithdrawalReasonRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional(readOnly = true)
    fun getProfile(userId: Long): MyProfileResponse {
        val user = userRepository.findById(userId).orElseThrow { UserNotFoundException(userId) }
        requireProfileSetupStarted(user)
        return user.toProfileResponse()
    }

    @Transactional(readOnly = true)
    fun getPublicProfile(userId: Long): PublicProfileResponse {
        val user = userRepository.findById(userId).orElseThrow { UserNotFoundException(userId) }
        val information = user.information ?: throw UserNotFoundException(userId)
        if (user.isWithdrawn()) throw UserNotFoundException(userId)
        return PublicProfileResponse(
            userId = user.id,
            nickname = information.nickname,
            nicknameColor = information.nicknameColor,
            profileImageUrl = information.profileFileName?.let(objectStorageRepository::getDownloadUrl),
            introduction = information.introduction,
            travelStyles = user.travelStyles.sortedBy { it.id }.map { TravelStyleResponse(it.id, it.label) },
            interestedRegions = user.interestedRegions.sortedBy { it.id }.map { InterestedRegionResponse(it.id, it.signguName) },
            mannerRating = user.mannerRating,
            completedTripCount = chatRoomParticipantRepository.countCompletedTrips(user.id),
            feedCount = feedRepository.countByAuthorIdAndVisibility(user.id, FeedVisibility.PUBLIC),
        )
    }

    @Transactional
    fun updateProfile(
        userId: Long,
        request: UpdateProfileRequest,
    ): MyProfileResponse {
        val user = userRepository.findByIdForUpdate(userId) ?: throw UserNotFoundException(userId)
        requireProfileSetupStarted(user)
        if (Period.between(request.birthDate, LocalDate.now()).years < AgePolicy.MINIMUM_SIGNUP_AGE) {
            throw BaseException(ErrorCode.MINIMUM_SIGNUP_AGE_NOT_MET)
        }
        val interestedRegions = legalDongCodeRepository.findAllById(request.interestedRegionIds)
        if (
            interestedRegions.map { it.id }.toSet() != request.interestedRegionIds ||
            interestedRegions.any { it.regionCode != GYEONGSANGBUKDO_REGION_CODE }
        ) {
            throw BaseException(ErrorCode.INVALID_INTERESTED_REGION_SELECTION)
        }
        val travelStyles = travelStyleRepository.findAllById(request.travelStyleIds)
        if (travelStyles.map { it.id }.toSet() != request.travelStyleIds) {
            throw BaseException(ErrorCode.INVALID_TRAVEL_STYLE_SELECTION)
        }
        user.updateProfile(
            introduction = request.introduction?.trim()?.takeIf(String::isNotEmpty),
            travelStyles = travelStyles.toSet(),
            interestedRegions = interestedRegions.toSet(),
            birthDate = request.birthDate,
            gender = request.gender,
        )
        return user.toProfileResponse()
    }

    @Transactional(readOnly = true)
    fun getProfileOptions(): ProfileOptionsResponse =
        ProfileOptionsResponse(
            travelStyles =
                travelStyleRepository
                    .findAllByOrderByLabelAsc()
                    .map { TravelStyleResponse(it.id, it.label) },
            interestedRegions =
                legalDongCodeRepository
                    .findAllByRegionCodeOrderBySignguNameAsc(GYEONGSANGBUKDO_REGION_CODE)
                    .map { InterestedRegionResponse(it.id, it.signguName) },
        )

    @Transactional
    fun generateProfileImage(userId: Long): ProfileImageGenerationResponse {
        val user = userRepository.findByIdForUpdate(userId) ?: throw UserNotFoundException(userId)
        requireProfileImageSelectionPending(user)
        if (!user.canGenerateProfileImage()) {
            throw BaseException(
                ErrorCode.PROFILE_IMAGE_GENERATION_LIMIT,
                ErrorCode.PROFILE_IMAGE_GENERATION_LIMIT.errorMessage,
            )
        }

        val information = checkNotNull(user.information)
        val prompt =
            profileImagePromptFactory.create(
                nickname = information.nickname,
                color = information.nicknameColor,
                userId = user.id,
                generationNumber = user.profileImageGenerationCount + 1,
            )
        val imageBytes = profileImageGenerationClient.generate(prompt)
        val generatedImageKey =
            objectStorageRepository.uploadGeneratedProfileImage(
                fhdWebpImageOptimizer.optimizeToFhdWebp(imageBytes, ErrorCode.PROFILE_IMAGE_GENERATION_FAILED),
            )
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
        requireProfileImageSelectionPending(user)
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
        requireProfileImageSelectionPending(user)
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

    /** 탈퇴 사유 선택지. 문구 정본은 서버에 둔다 — 세 플랫폼이 하드코딩하면 조금씩 어긋난다. */
    fun getWithdrawalReasons(): List<WithdrawalReasonResponse> =
        WithdrawalReason.entries.map { WithdrawalReasonResponse(reason = it, displayName = it.displayName) }

    /**
     * @param reason 탈퇴 화면에서 고른 사유. 선택값이다 — 예전에는 화면이 사유를 묻고도
     * 보낼 곳이 없어 그대로 버렸다 (QA `BE-25`). **누가 골랐는지는 남기지 않는다.**
     * @param reasonDetail 「기타」를 고른 경우의 자유 입력.
     */
    @Transactional
    fun withdraw(
        userId: Long,
        withdrawnAt: LocalDateTime = LocalDateTime.now(),
        reason: WithdrawalReason? = null,
        reasonDetail: String? = null,
    ) {
        val user = userRepository.findByIdForUpdate(userId) ?: throw UserNotFoundException(userId)
        if (user.isWithdrawn()) throw UserNotFoundException(userId)

        val activityObjectKeys =
            userWithdrawalDataRepository
                .removePersonalActivity(userId, user.information?.nickname, withdrawnAt)
                .all

        user.withdraw(withdrawnAt)
        // 사유는 **탈퇴 처리와 별개로** 남긴다. 사유 저장이 실패한다고 탈퇴를 막으면 안 되고,
        // 반대로 사유를 안 골랐다고 탈퇴를 막을 이유도 없다.
        reason?.let {
            withdrawalReasonRepository.save(
                WithdrawalReasonRecord(
                    reason = it,
                    // 「기타」가 아닌데 딸려 온 자유 입력은 버린다 — 화면이 지운 값을 서버가 주워 담지 않는다.
                    detail = reasonDetail?.trim()?.takeIf { text -> text.isNotEmpty() && it == WithdrawalReason.OTHER },
                ),
            )
        }
        scheduleWithdrawalCleanupAfterCommit(userId, activityObjectKeys)
    }

    @Transactional
    fun handleWithdrawnLogin(user: User): WithdrawnLoginResult = handleWithdrawnLogin(user, LocalDateTime.now())

    @Transactional
    fun handleWithdrawnLogin(
        user: User,
        loginAt: LocalDateTime,
    ): WithdrawnLoginResult {
        if (!user.isWithdrawn()) return WithdrawnLoginResult.ACTIVE
        if (user.canRestore(loginAt)) {
            user.restore()
            return WithdrawnLoginResult.RESTORED
        }
        permanentlyDelete(user, loginAt)
        return WithdrawnLoginResult.EXPIRED_DELETED
    }

    @Transactional
    fun deleteExpiredWithdrawnUsers(): Int = deleteExpiredWithdrawnUsers(LocalDateTime.now())

    @Transactional
    fun deleteExpiredWithdrawnUsers(now: LocalDateTime): Int {
        val cutoff = now.minusDays(User.WITHDRAWAL_GRACE_PERIOD_DAYS)
        val expiredUsers = userRepository.findAllByWithdrawnDateTimeLessThanEqual(cutoff)
        expiredUsers.forEach { permanentlyDelete(it, now) }
        return expiredUsers.size
    }

    private fun requireProfileSetupStarted(user: User) {
        if (user.information == null) {
            throw BaseException(ErrorCode.USER_INFO_REQUIRED, ErrorCode.USER_INFO_REQUIRED.errorMessage)
        }
    }

    private fun requireProfileImageSelectionPending(user: User) {
        requireProfileSetupStarted(user)
        if (user.signupState == SignupState.SIGNUP_COMPLETE || user.information?.profileFileName != null) {
            throw BaseException(
                ErrorCode.PROFILE_IMAGE_ALREADY_SELECTED,
                ErrorCode.PROFILE_IMAGE_ALREADY_SELECTED.errorMessage,
            )
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
        val notificationSetting = notificationSettingRepository.findByUserId(id)
        return MyProfileResponse(
            userId = id,
            nickname = information.nickname,
            nicknameColor = information.nicknameColor,
            profileImageUrl = information.profileFileName?.let(objectStorageRepository::getDownloadUrl),
            introduction = information.introduction,
            travelStyles = travelStyles.sortedBy { it.label }.map { TravelStyleResponse(it.id, it.label) },
            interestedRegions = interestedRegions.sortedBy { it.signguName }.map { InterestedRegionResponse(it.id, it.signguName) },
            birthDate = information.birthDate,
            gender = information.gender,
            chatNotificationMode = notificationSetting?.chatNotificationMode ?: ChatNotificationMode.ALL,
            recruitmentDeadlineEnabled = notificationSetting?.recruitmentDeadlineEnabled ?: true,
            socialActivityEnabled = notificationSetting?.socialActivityEnabled ?: true,
            marketingEnabled = notificationSetting?.marketingEnabled ?: true,
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
        objectKeys: List<String>,
    ) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            cleanupWithdrawalResources(userId, objectKeys)
            return
        }
        TransactionSynchronizationManager.registerSynchronization(
            object : TransactionSynchronization {
                override fun afterCommit() {
                    cleanupWithdrawalResources(userId, objectKeys)
                }
            },
        )
    }

    private fun cleanupWithdrawalResources(
        userId: Long,
        objectKeys: List<String>,
    ) {
        objectKeys.distinct().forEach(::deleteQuietly)
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

    private fun permanentlyDelete(
        user: User,
        deletedAt: LocalDateTime,
    ) {
        val userId = user.id
        val profileImageKeys = userProfileImageRepository.findFileNamesByUserIdOrderByCreatedDateTimeAsc(userId)
        val activityObjectKeys =
            userWithdrawalDataRepository
                .removePersonalActivity(userId, user.information?.nickname, deletedAt)
                .all
        val hostedRoomObjectKeys =
            userWithdrawalDataRepository
                .preparePermanentDeletion(userId, user.information?.nickname)
                .all

        userRepository.delete(user)
        scheduleWithdrawalCleanupAfterCommit(userId, profileImageKeys + activityObjectKeys + hostedRoomObjectKeys)
    }

    companion object {
        private const val GYEONGSANGBUKDO_REGION_CODE = "47"
    }
}

enum class WithdrawnLoginResult {
    ACTIVE,
    RESTORED,
    EXPIRED_DELETED,
}
