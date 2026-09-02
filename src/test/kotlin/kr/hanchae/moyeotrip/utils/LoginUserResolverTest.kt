package kr.hanchae.moyeotrip.utils

import kr.hanchae.moyeotrip.config.security.CustomUserDto
import kr.hanchae.moyeotrip.entity.user.SignupState
import kr.hanchae.moyeotrip.exception.BaseException
import kr.hanchae.moyeotrip.exception.ErrorCode
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.core.MethodParameter
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.context.request.NativeWebRequest

class LoginUserResolverTest {
    private val resolver = LoginUserResolver()

    @AfterEach
    fun clearContext() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `LoginUserId가 붙은 Long 파라미터만 지원한다`() {
        assertTrue(resolver.supportsParameter(parameter("loginUser", Long::class.javaPrimitiveType!!)))
        assertFalse(resolver.supportsParameter(parameter("plainLong", Long::class.javaPrimitiveType!!)))
        assertFalse(resolver.supportsParameter(parameter("wrongType", String::class.java)))
    }

    @Test
    fun `인증 principal의 사용자 ID를 반환한다`() {
        val principal =
            CustomUserDto(
                id = "42",
                password = "",
                authorities = listOf(SimpleGrantedAuthority("ROLE_USER")),
                signupState = SignupState.SIGNUP_COMPLETE,
                hasProfileImage = true,
            )
        SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken(principal, null, principal.authorities)

        val result = resolver.resolveArgument(parameter("loginUser", Long::class.javaPrimitiveType!!), null, mockRequest(), null)

        assertEquals(42L, result)
    }

    @Test
    fun `인증이 없거나 ID가 숫자가 아니면 인증 오류를 반환한다`() {
        val missing =
            assertThrows(BaseException::class.java) {
                resolver.resolveArgument(parameter("loginUser", Long::class.javaPrimitiveType!!), null, mockRequest(), null)
            }
        assertEquals(ErrorCode.UNAUTHORIZED, missing.errorCode)

        val principal =
            CustomUserDto(
                id = "not-a-number",
                password = "",
                authorities = emptyList(),
                signupState = SignupState.SIGNUP_COMPLETE,
                hasProfileImage = false,
            )
        SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken(principal, null)

        val malformed =
            assertThrows(NumberFormatException::class.java) {
                resolver.resolveArgument(parameter("loginUser", Long::class.javaPrimitiveType!!), null, mockRequest(), null)
            }
        assertTrue(malformed.message.orEmpty().contains("not-a-number"))
    }

    private fun parameter(
        methodName: String,
        type: Class<*>,
    ): MethodParameter = MethodParameter(Handler::class.java.getDeclaredMethod(methodName, type), 0)

    private fun mockRequest(): NativeWebRequest = org.mockito.Mockito.mock(NativeWebRequest::class.java)

    @Suppress("UNUSED_PARAMETER")
    private class Handler {
        fun loginUser(
            @LoginUserId userId: Long,
        ) = Unit

        fun plainLong(userId: Long) = Unit

        fun wrongType(
            @LoginUserId userId: String,
        ) = Unit
    }
}
