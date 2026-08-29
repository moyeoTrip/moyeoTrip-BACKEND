package kr.hanchae.moyeotrip.config.security

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import jakarta.servlet.FilterChain
import kr.hanchae.moyeotrip.config.properties.JwtProperties
import kr.hanchae.moyeotrip.entity.user.Gender
import kr.hanchae.moyeotrip.entity.user.NicknameColor
import kr.hanchae.moyeotrip.entity.user.SignupState
import kr.hanchae.moyeotrip.entity.user.User
import kr.hanchae.moyeotrip.entity.user.UserInformation
import kr.hanchae.moyeotrip.entity.user.UserRole
import kr.hanchae.moyeotrip.repository.UserRepository
import kr.hanchae.moyeotrip.utils.jwt.JwtUtil
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.redisson.api.RedissonClient
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.HttpMethod
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import java.util.Base64
import java.util.Optional

class JwtFilterTest {
    private val userRepository = mock(UserRepository::class.java)
    private val userDetailService = CustomUserDetailService(userRepository)
    private val jwtUtil =
        JwtUtil(
            JwtProperties(
                accessKey = Base64.getEncoder().encodeToString(ByteArray(32) { 1 }),
                refreshKey = Base64.getEncoder().encodeToString(ByteArray(32) { 2 }),
                accessTokenExpirationTime = 60_000,
                refreshTokenExpirationTime = 60_000,
            ),
            mock(RedissonClient::class.java),
        )
    private val objectMapper = jacksonObjectMapper()
    private val filter = JwtFilter(jwtUtil, userDetailService, objectMapper)

    @AfterEach
    fun clearSecurityContext() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `프로필 이미지가 없는 인증 사용자의 일반 API 요청은 40918로 차단한다`() {
        saveUser(SignupState.PROFILE_IMAGE_REQUIRED, profileFileName = null)
        val request = authenticatedRequest(HttpMethod.GET, "/api/v1/chat-rooms/my")
        val response = MockHttpServletResponse()
        val chain = mock(FilterChain::class.java)

        filter.doFilter(request, response, chain)

        assertEquals(409, response.status)
        assertEquals(40918, objectMapper.readTree(response.contentAsString).path("code").asInt())
        verify(chain, never()).doFilter(request, response)
    }

    @Test
    fun `가입 완료 값이 잘못 저장됐어도 프로필 파일이 없으면 일반 API 요청을 차단한다`() {
        saveUser(SignupState.SIGNUP_COMPLETE, profileFileName = null)
        val request = authenticatedRequest(HttpMethod.GET, "/api/v1/notifications")
        val response = MockHttpServletResponse()
        val chain = mock(FilterChain::class.java)

        filter.doFilter(request, response, chain)

        assertEquals(40918, objectMapper.readTree(response.contentAsString).path("code").asInt())
        verify(chain, never()).doFilter(request, response)
    }

    @Test
    fun `프로필 이미지가 없으면 내 프로필 API도 40918로 차단한다`() {
        saveUser(SignupState.PROFILE_IMAGE_REQUIRED, profileFileName = null)
        val request = authenticatedRequest(HttpMethod.GET, "/api/v1/users/me/profile")
        val response = MockHttpServletResponse()
        val chain = mock(FilterChain::class.java)

        filter.doFilter(request, response, chain)

        assertEquals(40918, objectMapper.readTree(response.contentAsString).path("code").asInt())
        verify(chain, never()).doFilter(request, response)
    }

    @Test
    fun `API 접두사가 달라도 프로필 없는 인증 사용자의 보호 요청은 기본 차단한다`() {
        saveUser(SignupState.PROFILE_IMAGE_REQUIRED, profileFileName = null)
        val request = authenticatedRequest(HttpMethod.GET, "/internal/protected-resource")
        val response = MockHttpServletResponse()
        val chain = mock(FilterChain::class.java)

        filter.doFilter(request, response, chain)

        assertEquals(40918, objectMapper.readTree(response.contentAsString).path("code").asInt())
        verify(chain, never()).doFilter(request, response)
    }

    @Test
    fun `사용자 정보 입력 전인 인증 사용자의 일반 API 요청은 40902로 차단한다`() {
        saveUser(SignupState.USER_INFO_REQUIRED, profileFileName = null)
        val request = authenticatedRequest(HttpMethod.GET, "/api/v1/chat-rooms/my")
        val response = MockHttpServletResponse()
        val chain = mock(FilterChain::class.java)

        filter.doFilter(request, response, chain)

        assertEquals(40902, objectMapper.readTree(response.contentAsString).path("code").asInt())
        verify(chain, never()).doFilter(request, response)
    }

    @Test
    fun `프로필 이미지 생성 조회 선택 API는 이미지가 없어도 JWT 인증 후 통과한다`() {
        saveUser(SignupState.PROFILE_IMAGE_REQUIRED, profileFileName = null)
        val chain = mock(FilterChain::class.java)
        val exchanges =
            listOf(
                authenticatedRequest(HttpMethod.POST, "/api/v1/users/me/profile-images") to MockHttpServletResponse(),
                authenticatedRequest(HttpMethod.GET, "/api/v1/users/me/profile-images") to MockHttpServletResponse(),
                authenticatedRequest(HttpMethod.PUT, "/api/v1/users/me/profile-image") to MockHttpServletResponse(),
            )

        exchanges.forEach { (request, response) -> filter.doFilter(request, response, chain) }

        exchanges.forEach { (request, response) -> verify(chain).doFilter(request, response) }
    }

    @Test
    fun `프로필 이미지 설정을 완료하면 생성 조회 선택 API를 모두 40919로 차단한다`() {
        saveUser(SignupState.SIGNUP_COMPLETE, profileFileName = "user/profile/image/selected.webp")
        val chain = mock(FilterChain::class.java)
        val exchanges =
            listOf(
                authenticatedRequest(HttpMethod.POST, "/api/v1/users/me/profile-images") to MockHttpServletResponse(),
                authenticatedRequest(HttpMethod.GET, "/api/v1/users/me/profile-images") to MockHttpServletResponse(),
                authenticatedRequest(HttpMethod.PUT, "/api/v1/users/me/profile-image") to MockHttpServletResponse(),
            )

        exchanges.forEach { (request, response) -> filter.doFilter(request, response, chain) }

        exchanges.forEach { (request, response) ->
            assertEquals(40919, objectMapper.readTree(response.contentAsString).path("code").asInt())
            verify(chain, never()).doFilter(request, response)
        }
    }

    @Test
    fun `선택된 프로필 이미지가 있는 가입 완료 사용자는 일반 API 요청을 통과한다`() {
        saveUser(SignupState.SIGNUP_COMPLETE, profileFileName = "user/profile/image/selected.webp")
        val request = authenticatedRequest(HttpMethod.GET, "/api/v1/chat-rooms/my")
        val response = MockHttpServletResponse()
        val chain = mock(FilterChain::class.java)

        filter.doFilter(request, response, chain)

        verify(chain).doFilter(request, response)
    }

    @Test
    fun `토큰이 없는 요청은 프로필 검사 없이 다음 보안 필터로 넘긴다`() {
        val request = MockHttpServletRequest(HttpMethod.GET.name(), "/api/v1/chat-rooms/my")
        val response = MockHttpServletResponse()
        val chain = mock(FilterChain::class.java)

        filter.doFilter(request, response, chain)

        verify(chain).doFilter(request, response)
    }

    private fun saveUser(
        signupState: SignupState,
        profileFileName: String?,
    ) {
        val user =
            User(
                id = USER_ID,
                userRole = UserRole.ROLE_USER,
                signupState = signupState,
                userInformation =
                    UserInformation(
                        nickname = "따뜻한 사슴 1234",
                        nicknameColor = NicknameColor.GREEN,
                        gender = Gender.F,
                        profileFileName = profileFileName,
                    ),
            )
        `when`(userRepository.findById(USER_ID)).thenReturn(Optional.of(user))
    }

    private fun authenticatedRequest(
        method: HttpMethod,
        path: String,
    ): MockHttpServletRequest =
        MockHttpServletRequest(method.name(), path).also {
            it.addHeader(AUTHORIZATION, "Bearer ${jwtUtil.generateAccessToken(USER_ID, "따뜻한 사슴 1234")}")
        }

    companion object {
        private const val USER_ID = 7L
    }
}
