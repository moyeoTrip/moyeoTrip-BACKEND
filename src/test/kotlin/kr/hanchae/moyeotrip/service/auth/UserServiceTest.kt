package kr.hanchae.moyeotrip.service.auth

import kr.hanchae.moyeotrip.client.ProfileImageGenerationClient
import kr.hanchae.moyeotrip.client.ProfileImagePromptFactory
import kr.hanchae.moyeotrip.controller.user.request.UpdateProfileRequest
import kr.hanchae.moyeotrip.entity.feed.FeedVisibility
import kr.hanchae.moyeotrip.entity.notification.ChatNotificationMode
import kr.hanchae.moyeotrip.entity.notification.NotificationSetting
import kr.hanchae.moyeotrip.entity.tour.LegalDongCode
import kr.hanchae.moyeotrip.entity.user.Gender
import kr.hanchae.moyeotrip.entity.user.NicknameColor
import kr.hanchae.moyeotrip.entity.user.SignupState
import kr.hanchae.moyeotrip.entity.user.TravelStyle
import kr.hanchae.moyeotrip.entity.user.User
import kr.hanchae.moyeotrip.entity.user.UserInformation
import kr.hanchae.moyeotrip.entity.user.UserProfileImage
import kr.hanchae.moyeotrip.entity.user.UserRole
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
import kr.hanchae.moyeotrip.utils.FhdWebpImageOptimizer
import kr.hanchae.moyeotrip.utils.jwt.JwtUtil
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Optional

class UserServiceTest {
    private val userRepository = mock(UserRepository::class.java)
    private val objectStorageRepository = mock(ObjectStorageRepository::class.java)
    private val profileImageGenerationClient = mock(ProfileImageGenerationClient::class.java)
    private val userProfileImageRepository = mock(UserProfileImageRepository::class.java)
    private val legalDongCodeRepository = mock(LegalDongCodeRepository::class.java)
    private val travelStyleRepository = mock(TravelStyleRepository::class.java)
    private val notificationSettingRepository = mock(NotificationSettingRepository::class.java)
    private val fhdWebpImageOptimizer = mock(FhdWebpImageOptimizer::class.java)
    private val promptFactory = ProfileImagePromptFactory()
    private val jwtUtil = mock(JwtUtil::class.java)
    private val userWithdrawalDataRepository = mock(UserWithdrawalDataRepository::class.java)
    private val chatRoomParticipantRepository = mock(ChatRoomParticipantRepository::class.java)
    private val feedRepository = mock(FeedRepository::class.java)
    private val service =
        UserService(
            userRepository,
            objectStorageRepository,
            profileImageGenerationClient,
            promptFactory,
            userProfileImageRepository,
            legalDongCodeRepository,
            travelStyleRepository,
            notificationSettingRepository,
            fhdWebpImageOptimizer,
            jwtUtil,
            userWithdrawalDataRepository,
            chatRoomParticipantRepository,
            feedRepository,
        )

    @Test
    fun `내 프로필에 기본 알림 수신 설정과 방해 금지 설정을 함께 반환한다`() {
        val user = profileImageRequiredUser()
        val notificationSetting =
            NotificationSetting(
                user = user,
                chatNotificationMode = ChatNotificationMode.MENTIONS_AND_REPLIES,
                recruitmentDeadlineEnabled = false,
                socialActivityEnabled = true,
                marketingEnabled = false,
            )
        `when`(userRepository.findById(7L)).thenReturn(Optional.of(user))
        `when`(notificationSettingRepository.findByUserId(7L)).thenReturn(notificationSetting)

        val response = service.getProfile(7L)

        assertEquals(7L, response.userId)
        assertEquals(NicknameColor.BLUE, response.nicknameColor)
        assertEquals(ChatNotificationMode.MENTIONS_AND_REPLIES, response.chatNotificationMode)
        assertFalse(response.recruitmentDeadlineEnabled)
        assertFalse(response.marketingEnabled)
    }

    @Test
    fun `이미지를 생성해도 현재 프로필에는 적용하지 않고 후보로 보관한다`() {
        val user = profileImageRequiredUser()
        val imageBytes = byteArrayOf(1, 2, 3)
        val optimizedImageBytes = byteArrayOf(4, 5, 6)
        val imageKey = "user/profile/image/generated.webp"
        val prompt = promptFactory.create("따스한 사슴 2347", NicknameColor.BLUE)
        `when`(userRepository.findByIdForUpdate(7L)).thenReturn(user)
        `when`(profileImageGenerationClient.generate(prompt)).thenReturn(imageBytes)
        `when`(
            fhdWebpImageOptimizer.optimizeToFhdWebp(imageBytes, ErrorCode.PROFILE_IMAGE_GENERATION_FAILED),
        ).thenReturn(optimizedImageBytes)
        `when`(objectStorageRepository.uploadGeneratedProfileImage(optimizedImageBytes)).thenReturn(imageKey)
        `when`(userProfileImageRepository.save(any(UserProfileImage::class.java)))
            .thenReturn(UserProfileImage(id = 12L, user = user, fileName = imageKey))
        `when`(objectStorageRepository.getDownloadUrl(imageKey))
            .thenReturn("https://cdn.example.com/user/profile/image/generated.webp")

        val response = service.generateProfileImage(7L)

        assertEquals(12L, response.candidate.profileImageId)
        assertFalse(response.candidate.selected)
        assertEquals(1, response.generationCount)
        assertEquals(2, response.remainingGenerationCount)
        assertEquals(SignupState.PROFILE_IMAGE_REQUIRED, response.signupState)
        assertNull(user.information?.profileFileName)
        assertEquals(SignupState.PROFILE_IMAGE_REQUIRED, user.signupState)
    }

    @Test
    fun `프로필 이미지 생성 트랜잭션이 롤백되면 업로드한 객체를 삭제한다`() {
        val user = profileImageRequiredUser()
        val imageBytes = byteArrayOf(1, 2, 3)
        val optimizedImageBytes = byteArrayOf(4, 5, 6)
        val imageKey = "user/profile/image/rollback.webp"
        val prompt = promptFactory.create("따스한 사슴 2347", NicknameColor.BLUE)
        `when`(userRepository.findByIdForUpdate(7L)).thenReturn(user)
        `when`(profileImageGenerationClient.generate(prompt)).thenReturn(imageBytes)
        `when`(
            fhdWebpImageOptimizer.optimizeToFhdWebp(imageBytes, ErrorCode.PROFILE_IMAGE_GENERATION_FAILED),
        ).thenReturn(optimizedImageBytes)
        `when`(objectStorageRepository.uploadGeneratedProfileImage(optimizedImageBytes)).thenReturn(imageKey)
        `when`(userProfileImageRepository.save(any(UserProfileImage::class.java)))
            .thenReturn(UserProfileImage(id = 12L, user = user, fileName = imageKey))
        `when`(objectStorageRepository.getDownloadUrl(imageKey)).thenReturn("https://cdn.example.com/rollback.webp")
        TransactionSynchronizationManager.initSynchronization()

        try {
            service.generateProfileImage(7L)

            verify(objectStorageRepository, never()).delete(imageKey)
            TransactionSynchronizationManager.getSynchronizations().single().afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK)
            verify(objectStorageRepository).delete(imageKey)
        } finally {
            TransactionSynchronizationManager.clearSynchronization()
        }
    }

    @Test
    fun `생성한 후보를 선택하면 프로필에 적용하고 회원가입을 완료한다`() {
        val user = profileImageRequiredUser()
        val image = UserProfileImage(id = 12L, user = user, fileName = "user/profile/image/generated.webp")
        `when`(userRepository.findByIdForUpdate(7L)).thenReturn(user)
        `when`(userProfileImageRepository.findByIdAndUserId(12L, 7L)).thenReturn(image)
        `when`(objectStorageRepository.getDownloadUrl(image.fileName)).thenReturn("https://cdn.example.com/generated.webp")

        val response = service.selectProfileImage(7L, 12L)

        assertTrue(response.selectedImage.selected)
        assertEquals(SignupState.SIGNUP_COMPLETE, response.signupState)
        assertEquals(image.fileName, user.information?.profileFileName)
        assertEquals(SignupState.SIGNUP_COMPLETE, user.signupState)
    }

    @Test
    fun `기존 후보 목록은 회원가입을 중단했다가 이어서 진행해도 조회할 수 있다`() {
        val user = profileImageRequiredUser().also { it.recordProfileImageGeneration() }
        val image = UserProfileImage(id = 12L, user = user, fileName = "user/profile/image/generated.webp")
        `when`(userRepository.findById(7L)).thenReturn(Optional.of(user))
        `when`(userProfileImageRepository.findAllByUserIdOrderByCreatedDateTimeAsc(7L)).thenReturn(listOf(image))
        `when`(objectStorageRepository.getDownloadUrl(image.fileName)).thenReturn("https://cdn.example.com/generated.webp")

        val response = service.getProfileImages(7L)

        assertEquals(listOf(12L), response.candidates.map { it.profileImageId })
        assertFalse(response.candidates.single().selected)
        assertEquals(1, response.generationCount)
    }

    @Test
    fun `다른 사용자의 이미지 후보는 선택할 수 없다`() {
        val user = profileImageRequiredUser()
        `when`(userRepository.findByIdForUpdate(7L)).thenReturn(user)
        `when`(userProfileImageRepository.findByIdAndUserId(99L, 7L)).thenReturn(null)

        val exception = assertThrows(BaseException::class.java) { service.selectProfileImage(7L, 99L) }

        assertEquals(ErrorCode.PROFILE_IMAGE_NOT_FOUND, exception.errorCode)
    }

    @Test
    fun `프로필 이미지를 3번 생성한 사용자는 추가 생성을 거부한다`() {
        val user = profileImageRequiredUser()
        repeat(3) { user.recordProfileImageGeneration() }
        `when`(userRepository.findByIdForUpdate(7L)).thenReturn(user)

        val exception = assertThrows(BaseException::class.java) { service.generateProfileImage(7L) }

        assertEquals(ErrorCode.PROFILE_IMAGE_GENERATION_LIMIT, exception.errorCode)
        verifyNoInteractions(profileImageGenerationClient)
        verifyNoInteractions(objectStorageRepository)
        verifyNoInteractions(userProfileImageRepository)
    }

    @Test
    fun `회원 탈퇴는 활동 데이터를 삭제하고 계정을 30일 복구 상태로 전환한다`() {
        val user = profileImageRequiredUser()
        val feedImageKey = "feed/image/first.png"
        val chatImageKey = "chat/image/second.png"
        val withdrawnAt = LocalDateTime.of(2026, 8, 24, 12, 0)
        `when`(userRepository.findByIdForUpdate(7L)).thenReturn(user)
        `when`(
            userWithdrawalDataRepository.removePersonalActivity(
                7L,
                "따스한 사슴 2347",
                withdrawnAt,
            ),
        ).thenReturn(UserWithdrawalDataRepository.StoredObjectKeys(listOf(feedImageKey), listOf(chatImageKey)))
        TransactionSynchronizationManager.initSynchronization()

        try {
            service.withdraw(7L, withdrawnAt)

            assertTrue(user.isWithdrawn())
            assertNull(user.fcmToken)
            verify(userRepository, never()).delete(user)
            verify(objectStorageRepository, never()).delete(feedImageKey)
            verify(jwtUtil, never()).deleteCachedRefreshTokenRotateId(7L)

            TransactionSynchronizationManager.getSynchronizations().single().afterCommit()

            verify(objectStorageRepository).delete(feedImageKey)
            verify(objectStorageRepository).delete(chatImageKey)
            verify(jwtUtil).deleteCachedRefreshTokenRotateId(7L)
        } finally {
            TransactionSynchronizationManager.clearSynchronization()
        }
    }

    @Test
    fun `존재하지 않는 사용자의 회원 탈퇴는 거부한다`() {
        `when`(userRepository.findByIdForUpdate(404L)).thenReturn(null)

        assertThrows(UserNotFoundException::class.java) { service.withdraw(404L) }

        verify(userRepository, never()).delete(any(User::class.java))
        verifyNoInteractions(userWithdrawalDataRepository)
        verifyNoInteractions(objectStorageRepository)
        verifyNoInteractions(jwtUtil)
    }

    @Test
    fun `탈퇴 후 30일 이내 로그인하면 계정을 복구한다`() {
        val user = profileImageRequiredUser()
        val withdrawnAt = LocalDateTime.of(2026, 8, 1, 12, 0)
        user.withdraw(withdrawnAt)

        val result = service.handleWithdrawnLogin(user, withdrawnAt.plusDays(29).plusHours(23))

        assertEquals(WithdrawnLoginResult.RESTORED, result)
        assertFalse(user.isWithdrawn())
        verify(userRepository, never()).delete(user)
        verifyNoInteractions(userWithdrawalDataRepository)
    }

    @Test
    fun `탈퇴 후 30일이 지나 로그인하면 계정을 영구 삭제한다`() {
        val user = profileImageRequiredUser()
        val withdrawnAt = LocalDateTime.of(2026, 8, 1, 12, 0)
        val expiredAt = withdrawnAt.plusDays(User.WITHDRAWAL_GRACE_PERIOD_DAYS)
        val profileImageKey = "user/profile/image/profile.webp"
        val feedImageKey = "feed/image/feed.webp"
        val roomImageKey = "chat/image/room.webp"
        user.withdraw(withdrawnAt)
        `when`(userProfileImageRepository.findFileNamesByUserIdOrderByCreatedDateTimeAsc(7L))
            .thenReturn(listOf(profileImageKey))
        `when`(userWithdrawalDataRepository.removePersonalActivity(7L, "따스한 사슴 2347", expiredAt))
            .thenReturn(UserWithdrawalDataRepository.StoredObjectKeys(listOf(feedImageKey), emptyList()))
        `when`(userWithdrawalDataRepository.preparePermanentDeletion(7L, "따스한 사슴 2347"))
            .thenReturn(UserWithdrawalDataRepository.StoredObjectKeys(emptyList(), listOf(roomImageKey)))

        val result = service.handleWithdrawnLogin(user, expiredAt)

        assertEquals(WithdrawnLoginResult.EXPIRED_DELETED, result)
        verify(userRepository).delete(user)
        verify(objectStorageRepository).delete(profileImageKey)
        verify(objectStorageRepository).delete(feedImageKey)
        verify(objectStorageRepository).delete(roomImageKey)
        verify(jwtUtil).deleteCachedRefreshTokenRotateId(7L)
    }

    @Test
    fun `스케줄러용 영구 삭제는 탈퇴 후 30일이 지난 사용자만 조회한다`() {
        val now = LocalDateTime.of(2026, 8, 31, 12, 0)
        val cutoff = now.minusDays(User.WITHDRAWAL_GRACE_PERIOD_DAYS)
        `when`(userRepository.findAllByWithdrawnDateTimeLessThanEqual(cutoff)).thenReturn(emptyList())

        val deletedCount = service.deleteExpiredWithdrawnUsers(now)

        assertEquals(0, deletedCount)
        verify(userRepository).findAllByWithdrawnDateTimeLessThanEqual(cutoff)
    }

    @Test
    fun `프로필의 자기소개 여행 스타일 관심 지역 생년월일 성별을 수정한다`() {
        val user = profileImageRequiredUser()
        val birthDate = LocalDate.now().minusYears(25)
        val andong = LegalDongCode(id = 1L, regionCode = "47", signguCode = "47170", regionName = "경상북도", signguName = "안동시")
        val pohang = LegalDongCode(id = 2L, regionCode = "47", signguCode = "47110", regionName = "경상북도", signguName = "포항시")
        val nature = TravelStyle(id = 1L, label = "자연")
        val photography = TravelStyle(id = 2L, label = "사진")
        `when`(userRepository.findByIdForUpdate(7L)).thenReturn(user)
        `when`(legalDongCodeRepository.findAllById(setOf(1L, 2L)))
            .thenReturn(listOf(andong, pohang))
        `when`(travelStyleRepository.findAllById(setOf(1L, 2L)))
            .thenReturn(listOf(nature, photography))

        val response =
            service.updateProfile(
                7L,
                UpdateProfileRequest(
                    introduction = "  느긋한 여행을 좋아해요  ",
                    travelStyleIds = setOf(1L, 2L),
                    interestedRegionIds = setOf(1L, 2L),
                    birthDate = birthDate,
                    gender = Gender.F,
                ),
            )

        assertEquals("느긋한 여행을 좋아해요", response.introduction)
        assertEquals(listOf(2L, 1L), response.travelStyles.map { it.id })
        assertEquals(listOf("사진", "자연"), response.travelStyles.map { it.label })
        assertEquals(setOf("안동시", "포항시"), response.interestedRegions.map { it.signguName }.toSet())
        assertEquals(birthDate, response.birthDate)
        assertEquals(Gender.F, response.gender)
    }

    @Test
    fun `존재하지 않는 관심 지역 ID가 포함되면 프로필 수정을 거부한다`() {
        val user = profileImageRequiredUser()
        val birthDate = LocalDate.now().minusYears(25)
        `when`(userRepository.findByIdForUpdate(7L)).thenReturn(user)
        `when`(legalDongCodeRepository.findAllById(setOf(1L, 99999L)))
            .thenReturn(
                listOf(
                    LegalDongCode(
                        id = 1L,
                        regionCode = "47",
                        signguCode = "47170",
                        regionName = "경상북도",
                        signguName = "안동시",
                    ),
                ),
            )

        val exception =
            assertThrows(BaseException::class.java) {
                service.updateProfile(
                    7L,
                    UpdateProfileRequest(
                        interestedRegionIds = setOf(1L, 99999L),
                        birthDate = birthDate,
                        gender = Gender.F,
                    ),
                )
            }

        assertEquals(ErrorCode.INVALID_INTERESTED_REGION_SELECTION, exception.errorCode)
        verifyNoInteractions(travelStyleRepository)
    }

    @Test
    fun `만 20세 미만 생년월일로 프로필을 수정할 수 없다`() {
        val user = profileImageRequiredUser()
        `when`(userRepository.findByIdForUpdate(7L)).thenReturn(user)

        val exception =
            assertThrows(BaseException::class.java) {
                service.updateProfile(
                    7L,
                    UpdateProfileRequest(
                        birthDate = LocalDate.now().minusYears(20).plusDays(1),
                        gender = Gender.F,
                    ),
                )
            }

        assertEquals(ErrorCode.MINIMUM_SIGNUP_AGE_NOT_MET, exception.errorCode)
        verifyNoInteractions(legalDongCodeRepository, travelStyleRepository)
    }

    @Test
    fun `경상북도에 없거나 존재하지 않는 관심 지역은 프로필에 저장할 수 없다`() {
        val user = profileImageRequiredUser()
        `when`(userRepository.findByIdForUpdate(7L)).thenReturn(user)
        `when`(legalDongCodeRepository.findAllById(setOf(99L))).thenReturn(emptyList())

        val exception =
            assertThrows(BaseException::class.java) {
                service.updateProfile(
                    7L,
                    UpdateProfileRequest(
                        interestedRegionIds = setOf(99L),
                        birthDate = LocalDate.now().minusYears(25),
                        gender = Gender.F,
                    ),
                )
            }

        assertEquals(ErrorCode.INVALID_INTERESTED_REGION_SELECTION, exception.errorCode)
        verifyNoInteractions(travelStyleRepository)
    }

    @Test
    fun `존재하지 않는 여행 스타일은 프로필에 저장할 수 없다`() {
        val user = profileImageRequiredUser()
        `when`(userRepository.findByIdForUpdate(7L)).thenReturn(user)
        `when`(legalDongCodeRepository.findAllById(emptySet())).thenReturn(emptyList())
        `when`(travelStyleRepository.findAllById(setOf(99L))).thenReturn(emptyList())

        val exception =
            assertThrows(BaseException::class.java) {
                service.updateProfile(
                    7L,
                    UpdateProfileRequest(
                        travelStyleIds = setOf(99L),
                        birthDate = LocalDate.now().minusYears(25),
                        gender = Gender.F,
                    ),
                )
            }

        assertEquals(ErrorCode.INVALID_TRAVEL_STYLE_SELECTION, exception.errorCode)
    }

    @Test
    fun `프로필 선택지는 이름 순 여행 스타일과 경상북도 시군을 반환한다`() {
        `when`(travelStyleRepository.findAllByOrderByLabelAsc())
            .thenReturn(listOf(TravelStyle(id = 2L, label = "사진"), TravelStyle(id = 1L, label = "자연")))
        `when`(legalDongCodeRepository.findAllByRegionCodeOrderBySignguNameAsc("47"))
            .thenReturn(
                listOf(
                    LegalDongCode(
                        id = 1L,
                        regionCode = "47",
                        signguCode = "47170",
                        regionName = "경상북도",
                        signguName = "안동시",
                    ),
                ),
            )

        val response = service.getProfileOptions()

        assertEquals(listOf("사진", "자연"), response.travelStyles.map { it.label })
        assertEquals(listOf("안동시"), response.interestedRegions.map { it.signguName })
    }

    @Test
    fun `다른 사용자 프로필에서는 공개 프로필 항목만 반환한다`() {
        val style = TravelStyle(id = 2L, label = "사진")
        val region = LegalDongCode(id = 3L, regionCode = "47", signguCode = "47170", regionName = "경상북도", signguName = "안동시")
        val user =
            User(
                id = 8L,
                userRole = UserRole.ROLE_USER,
                signupState = SignupState.SIGNUP_COMPLETE,
                userInformation =
                    UserInformation(
                        nickname = "여행자",
                        nicknameColor = NicknameColor.MINT,
                        gender = Gender.F,
                        birthDate = LocalDate.of(1998, 4, 12),
                        profileFileName = "profile.webp",
                        introduction = "함께 걸어요",
                    ),
            )
        user.updateProfile("함께 걸어요", setOf(style), setOf(region), LocalDate.of(1998, 4, 12), Gender.F)
        user.updateMannerRating(4.7)
        `when`(userRepository.findById(8L)).thenReturn(Optional.of(user))
        `when`(objectStorageRepository.getDownloadUrl("profile.webp")).thenReturn("https://cdn.example.com/profile.webp")
        `when`(chatRoomParticipantRepository.countCompletedTrips(8L)).thenReturn(6L)
        `when`(feedRepository.countByAuthorIdAndVisibility(8L, FeedVisibility.PUBLIC)).thenReturn(4L)

        val response = service.getPublicProfile(8L)

        assertEquals(8L, response.userId)
        assertEquals("여행자", response.nickname)
        assertEquals(NicknameColor.MINT, response.nicknameColor)
        assertEquals("함께 걸어요", response.introduction)
        assertEquals(listOf("사진"), response.travelStyles.map { it.label })
        assertEquals(listOf("안동시"), response.interestedRegions.map { it.signguName })
        assertEquals(4.7, response.mannerRating)
        assertEquals(6L, response.completedTripCount)
        assertEquals(4L, response.feedCount)
    }

    @Test
    fun `가입 정보 입력 전에는 프로필을 조회할 수 없다`() {
        val user = User(id = 7L, userRole = UserRole.ROLE_USER)
        `when`(userRepository.findById(7L)).thenReturn(Optional.of(user))

        val exception = assertThrows(BaseException::class.java) { service.getProfile(7L) }

        assertEquals(ErrorCode.USER_INFO_REQUIRED, exception.errorCode)
    }

    private fun profileImageRequiredUser(): User =
        User(
            id = 7L,
            userRole = UserRole.ROLE_USER,
            signupState = SignupState.PROFILE_IMAGE_REQUIRED,
            userInformation =
                UserInformation(
                    nickname = "따스한 사슴 2347",
                    nicknameColor = NicknameColor.BLUE,
                    gender = Gender.N,
                ),
        )
}
