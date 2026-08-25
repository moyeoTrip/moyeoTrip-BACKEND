package kr.hanchae.moyeotrip.config.security

import kr.hanchae.moyeotrip.entity.user.User
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
    }
}
