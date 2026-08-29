package kr.hanchae.moyeotrip.config.security

import kr.hanchae.moyeotrip.entity.user.Gender
import kr.hanchae.moyeotrip.entity.user.NicknameColor
import kr.hanchae.moyeotrip.entity.user.SignupState
import kr.hanchae.moyeotrip.entity.user.User
import kr.hanchae.moyeotrip.entity.user.UserInformation
import kr.hanchae.moyeotrip.entity.user.UserRole
import kr.hanchae.moyeotrip.exception.BaseException
import kr.hanchae.moyeotrip.exception.ErrorCode
import kr.hanchae.moyeotrip.repository.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.time.LocalDateTime
import java.util.Optional

class CustomUserDetailServiceTest {
    private val userRepository = mock(UserRepository::class.java)
    private val service = CustomUserDetailService(userRepository)

    @Test
    fun `탈퇴 유예 중인 사용자의 기존 액세스 토큰은 인증하지 않는다`() {
        val user = User(id = 7L, userRole = UserRole.ROLE_USER).also { it.withdraw(LocalDateTime.now()) }
        `when`(userRepository.findById(7L)).thenReturn(Optional.of(user))

        val exception = assertThrows(BaseException::class.java) { service.loadUserById(7L) }

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.errorCode)
    }

    @Test
    fun `활성 사용자는 사용자 권한으로 인증한다`() {
        val user = User(id = 7L, userRole = UserRole.ROLE_USER)
        `when`(userRepository.findById(7L)).thenReturn(Optional.of(user))

        val details = service.loadUserById(7L)

        assertEquals("7", details.username)
        assertEquals(listOf("ROLE_USER"), details.authorities.map { it.authority })
        assertEquals(SignupState.USER_INFO_REQUIRED, (details as CustomUserDto).signupState)
        assertEquals(false, details.hasProfileImage)
    }

    @Test
    fun `인증 사용자 정보에 현재 가입 상태와 선택한 프로필 이미지 여부를 포함한다`() {
        val user =
            User(
                id = 7L,
                userRole = UserRole.ROLE_USER,
                signupState = SignupState.PROFILE_IMAGE_REQUIRED,
                userInformation =
                    UserInformation(
                        nickname = "따뜻한 사슴 1234",
                        nicknameColor = NicknameColor.GREEN,
                        gender = Gender.F,
                    ),
            ).also { it.selectProfileImage("user/profile/image/selected.webp") }
        `when`(userRepository.findById(7L)).thenReturn(Optional.of(user))

        val details = service.loadUserById(7L) as CustomUserDto

        assertEquals(SignupState.SIGNUP_COMPLETE, details.signupState)
        assertEquals(true, details.hasProfileImage)
    }
}
