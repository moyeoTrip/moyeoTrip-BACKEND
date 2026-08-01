package kr.hanchae.moyeotrip.service.auth

import kr.hanchae.moyeotrip.client.ProfileImageGenerationClient
import kr.hanchae.moyeotrip.client.ProfileImagePromptFactory
import kr.hanchae.moyeotrip.entity.user.Gender
import kr.hanchae.moyeotrip.entity.user.NicknameColor
import kr.hanchae.moyeotrip.entity.user.SignupState
import kr.hanchae.moyeotrip.entity.user.User
import kr.hanchae.moyeotrip.entity.user.UserInformation
import kr.hanchae.moyeotrip.entity.user.UserProfileImage
import kr.hanchae.moyeotrip.entity.user.UserRole
import kr.hanchae.moyeotrip.exception.BaseException
import kr.hanchae.moyeotrip.exception.ErrorCode
import kr.hanchae.moyeotrip.repository.ObjectStorageRepository
import kr.hanchae.moyeotrip.repository.UserProfileImageRepository
import kr.hanchae.moyeotrip.repository.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import java.util.Optional

class UserServiceTest {
    private val userRepository = mock(UserRepository::class.java)
    private val objectStorageRepository = mock(ObjectStorageRepository::class.java)
    private val profileImageGenerationClient = mock(ProfileImageGenerationClient::class.java)
    private val userProfileImageRepository = mock(UserProfileImageRepository::class.java)
    private val promptFactory = ProfileImagePromptFactory()
    private val service =
        UserService(
            userRepository,
            objectStorageRepository,
            profileImageGenerationClient,
            promptFactory,
            userProfileImageRepository,
        )

    @Test
    fun `이미지를 생성해도 현재 프로필에는 적용하지 않고 후보로 보관한다`() {
        val user = profileImageRequiredUser()
        val imageBytes = byteArrayOf(1, 2, 3)
        val imageKey = "user/profile/image/generated.png"
        val prompt = promptFactory.create("따스한 사슴 2347", NicknameColor.BLUE)
        `when`(userRepository.findByIdForUpdate(7L)).thenReturn(user)
        `when`(profileImageGenerationClient.generate(prompt)).thenReturn(imageBytes)
        `when`(objectStorageRepository.uploadGeneratedProfileImage(imageBytes)).thenReturn(imageKey)
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
