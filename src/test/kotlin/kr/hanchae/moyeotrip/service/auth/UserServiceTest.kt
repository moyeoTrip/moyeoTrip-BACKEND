package kr.hanchae.moyeotrip.service.auth

import kr.hanchae.moyeotrip.client.ProfileImageGenerationClient
import kr.hanchae.moyeotrip.client.ProfileImagePromptFactory
import kr.hanchae.moyeotrip.controller.user.request.UpdateProfileRequest
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
import kr.hanchae.moyeotrip.repository.LegalDongCodeRepository
import kr.hanchae.moyeotrip.repository.NotificationSettingRepository
import kr.hanchae.moyeotrip.repository.ObjectStorageRepository
import kr.hanchae.moyeotrip.repository.TravelStyleRepository
import kr.hanchae.moyeotrip.repository.UserProfileImageRepository
import kr.hanchae.moyeotrip.repository.UserRepository
import kr.hanchae.moyeotrip.utils.ProfileImageOptimizer
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
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.time.LocalDate
import java.util.Optional

class UserServiceTest {
    private val userRepository = mock(UserRepository::class.java)
    private val objectStorageRepository = mock(ObjectStorageRepository::class.java)
    private val profileImageGenerationClient = mock(ProfileImageGenerationClient::class.java)
    private val userProfileImageRepository = mock(UserProfileImageRepository::class.java)
    private val legalDongCodeRepository = mock(LegalDongCodeRepository::class.java)
    private val travelStyleRepository = mock(TravelStyleRepository::class.java)
    private val notificationSettingRepository = mock(NotificationSettingRepository::class.java)
    private val profileImageOptimizer = mock(ProfileImageOptimizer::class.java)
    private val promptFactory = ProfileImagePromptFactory()
    private val jwtUtil = mock(JwtUtil::class.java)
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
            profileImageOptimizer,
            jwtUtil,
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

        assertEquals(ChatNotificationMode.MENTIONS_AND_REPLIES, response.chatNotificationMode)
        assertFalse(response.recruitmentDeadlineEnabled)
        assertFalse(response.marketingEnabled)
    }

    @Test
    fun `이미지를 생성해도 현재 프로필에는 적용하지 않고 후보로 보관한다`() {
        val user = profileImageRequiredUser()
        val imageBytes = byteArrayOf(1, 2, 3)
        val optimizedImageBytes = byteArrayOf(4, 5, 6)
        val imageKey = "user/profile/image/generated.png"
        val prompt = promptFactory.create("따스한 사슴 2347", NicknameColor.BLUE)
        `when`(userRepository.findByIdForUpdate(7L)).thenReturn(user)
        `when`(profileImageGenerationClient.generate(prompt)).thenReturn(imageBytes)
        `when`(profileImageOptimizer.optimizeToHdWebp(imageBytes)).thenReturn(optimizedImageBytes)
        `when`(objectStorageRepository.uploadGeneratedProfileImage(optimizedImageBytes)).thenReturn(imageKey)
        `when`(userProfileImageRepository.save(any(UserProfileImage::class.java)))
            .thenReturn(UserProfileImage(id = 12L, user = user, fileName = imageKey))
        `when`(objectStorageRepository.getDownloadUrl(imageKey))
            .thenReturn("https://cdn.example.com/user/profile/image/generated.png")

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
    fun `생성한 후보를 선택하면 프로필에 적용하고 회원가입을 완료한다`() {
        val user = profileImageRequiredUser()
        val image = UserProfileImage(id = 12L, user = user, fileName = "user/profile/image/generated.png")
        `when`(userRepository.findByIdForUpdate(7L)).thenReturn(user)
        `when`(userProfileImageRepository.findByIdAndUserId(12L, 7L)).thenReturn(image)
        `when`(objectStorageRepository.getDownloadUrl(image.fileName)).thenReturn("https://cdn.example.com/generated.png")

        val response = service.selectProfileImage(7L, 12L)

        assertTrue(response.selectedImage.selected)
        assertEquals(SignupState.SIGNUP_COMPLETE, response.signupState)
        assertEquals(image.fileName, user.information?.profileFileName)
        assertEquals(SignupState.SIGNUP_COMPLETE, user.signupState)
    }

    @Test
    fun `기존 후보 목록은 회원가입을 중단했다가 이어서 진행해도 조회할 수 있다`() {
        val user = profileImageRequiredUser().also { it.recordProfileImageGeneration() }
        val image = UserProfileImage(id = 12L, user = user, fileName = "user/profile/image/generated.png")
        `when`(userRepository.findById(7L)).thenReturn(Optional.of(user))
        `when`(userProfileImageRepository.findAllByUserIdOrderByCreatedDateTimeAsc(7L)).thenReturn(listOf(image))
        `when`(objectStorageRepository.getDownloadUrl(image.fileName)).thenReturn("https://cdn.example.com/generated.png")

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
    fun `회원 탈퇴는 사용자를 삭제하고 커밋 후 이미지와 Refresh Token을 정리한다`() {
        val user = profileImageRequiredUser()
        val firstImageKey = "user/profile/image/first.png"
        val secondImageKey = "user/profile/image/second.png"
        `when`(userRepository.findByIdForUpdate(7L)).thenReturn(user)
        `when`(userProfileImageRepository.findFileNamesByUserIdOrderByCreatedDateTimeAsc(7L))
            .thenReturn(listOf(firstImageKey, secondImageKey))
        TransactionSynchronizationManager.initSynchronization()

        try {
            service.withdraw(7L)

            verify(userRepository).delete(user)
            verify(objectStorageRepository, never()).delete(firstImageKey)
            verify(jwtUtil, never()).deleteCachedRefreshTokenRotateId(7L)

            TransactionSynchronizationManager.getSynchronizations().single().afterCommit()

            verify(objectStorageRepository).delete(firstImageKey)
            verify(objectStorageRepository).delete(secondImageKey)
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
        verifyNoInteractions(objectStorageRepository)
        verifyNoInteractions(jwtUtil)
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
