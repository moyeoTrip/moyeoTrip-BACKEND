package kr.hanchae.moyeotrip.service.auth

import kr.hanchae.moyeotrip.client.FirebaseAuthenticationClient
import kr.hanchae.moyeotrip.client.FirebaseIdentity
import kr.hanchae.moyeotrip.client.KakaoClient
import kr.hanchae.moyeotrip.config.properties.KakaoProperties
import kr.hanchae.moyeotrip.controller.auth.request.FirebaseLoginRequest
import kr.hanchae.moyeotrip.controller.auth.request.FirebaseSignupRequest
import kr.hanchae.moyeotrip.controller.auth.request.KakaoAuthorizationCodeRequest
import kr.hanchae.moyeotrip.controller.auth.request.KakaoCustomTokenRequest
import kr.hanchae.moyeotrip.controller.client.KakaoTokenInfoResponse
import kr.hanchae.moyeotrip.entity.notification.NotificationSetting
import kr.hanchae.moyeotrip.entity.user.Gender
import kr.hanchae.moyeotrip.entity.user.NicknameColor
import kr.hanchae.moyeotrip.entity.user.ProviderType
import kr.hanchae.moyeotrip.entity.user.SignupState
import kr.hanchae.moyeotrip.entity.user.User
import kr.hanchae.moyeotrip.entity.user.UserAuthIdentity
import kr.hanchae.moyeotrip.entity.user.UserRole
import kr.hanchae.moyeotrip.exception.BaseException
import kr.hanchae.moyeotrip.exception.ErrorCode
import kr.hanchae.moyeotrip.repository.NicknameCandidateRepository
import kr.hanchae.moyeotrip.repository.NotificationSettingRepository
import kr.hanchae.moyeotrip.repository.UserAuthIdentityRepository
import kr.hanchae.moyeotrip.repository.UserRepository
import kr.hanchae.moyeotrip.utils.jwt.JwtUtil
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException
import java.time.LocalDate

class AuthServiceTest {
    private lateinit var userRepository: UserRepository
    private lateinit var jwtUtil: JwtUtil
    private lateinit var firebaseAuthenticationClient: FirebaseAuthenticationClient
    private lateinit var kakaoClient: KakaoClient
    private lateinit var userAuthIdentityRepository: UserAuthIdentityRepository
    private lateinit var nicknameCandidateRepository: NicknameCandidateRepository
    private lateinit var notificationSettingRepository: NotificationSettingRepository
    private lateinit var authService: AuthService

    @BeforeEach
    fun setUp() {
        userRepository = mock(UserRepository::class.java)
        jwtUtil = mock(JwtUtil::class.java)
        firebaseAuthenticationClient = mock(FirebaseAuthenticationClient::class.java)
        kakaoClient = mock(KakaoClient::class.java)
        userAuthIdentityRepository = mock(UserAuthIdentityRepository::class.java)
        nicknameCandidateRepository = mock(NicknameCandidateRepository::class.java)
        notificationSettingRepository = mock(NotificationSettingRepository::class.java)
        authService =
            AuthService(
                userRepository,
                jwtUtil,
                firebaseAuthenticationClient,
                kakaoClient,
                KakaoProperties(
                    appId = 987654L,
                    restApiKey = "rest-api-key",
                    clientSecret = "client-secret",
                    allowedRedirectUris = listOf("https://moyeotrip.github.io/moyeoTrip-Web/auth/kakao/callback"),
                ),
                userAuthIdentityRepository,
                nicknameCandidateRepository,
                notificationSettingRepository,
            )
    }

    @Test
    fun `등록되지 않은 Firebase 사용자는 신규 사용자로 응답한다`() {
        val identity = FirebaseIdentity("firebase-uid", "user@example.com", ProviderType.EMAIL)
        `when`(firebaseAuthenticationClient.verifyIdToken("id-token")).thenReturn(identity)
        `when`(userAuthIdentityRepository.findByProviderTypeAndProviderUserId(ProviderType.EMAIL, "firebase-uid")).thenReturn(null)
        `when`(userRepository.findByEmail("user@example.com")).thenReturn(null)

        val response = authService.loginWithFirebase(FirebaseLoginRequest("id-token"))

        assertTrue(response.isNewUser)
        assertEquals(ProviderType.EMAIL, response.providerType)
        assertEquals(null, response.accessToken)
    }

    @Test
    fun `Firebase 회원가입은 완성된 사용자와 서비스 토큰을 생성한다`() {
        val minimumAgeBirthDate = LocalDate.now().minusYears(20)
        val identity = FirebaseIdentity("firebase-uid", "user@example.com", ProviderType.EMAIL)
        `when`(firebaseAuthenticationClient.verifyIdToken("id-token")).thenReturn(identity)
        `when`(nicknameCandidateRepository.consume("selection-token"))
            .thenReturn(
                mapOf(
                    "따스한 사슴 1234" to NicknameColor.RED,
                    "빠른 거북이 9999" to NicknameColor.BLUE,
                    "다정한 수달 5271" to NicknameColor.MINT,
                ),
            )
        `when`(userRepository.existsByInformationNickname("따스한 사슴 1234")).thenReturn(false)
        `when`(userAuthIdentityRepository.findByProviderTypeAndProviderUserId(ProviderType.EMAIL, "firebase-uid")).thenReturn(null)
        `when`(userRepository.save(any(User::class.java))).thenAnswer { it.arguments[0] as User }
        `when`(jwtUtil.generateAccessToken(0L, "따스한 사슴 1234")).thenReturn("access-token")
        `when`(jwtUtil.generateRotateId()).thenReturn("rotate-id")
        `when`(jwtUtil.generateRefreshToken(0L, "rotate-id")).thenReturn("refresh-token")

        val response =
            authService.signupWithFirebase(
                FirebaseSignupRequest(
                    idToken = "id-token",
                    nicknameSelectionToken = "selection-token",
                    nickname = "따스한 사슴 1234",
                    gender = Gender.F,
                    birthDate = minimumAgeBirthDate,
                    fcmToken = "fcm-token",
                ),
            )

        assertEquals("access-token", response.accessToken)
        assertEquals("refresh-token", response.refreshToken)
        val savedUser = org.mockito.ArgumentCaptor.forClass(User::class.java)
        verify(userRepository).save(savedUser.capture())
        assertEquals("따스한 사슴 1234", savedUser.value.information?.nickname)
        assertEquals(NicknameColor.RED, savedUser.value.information?.nicknameColor)
        assertEquals(Gender.F, savedUser.value.information?.gender)
        assertEquals(minimumAgeBirthDate, savedUser.value.information?.birthDate)
        assertEquals("fcm-token", savedUser.value.fcmToken)
        assertEquals(SignupState.PROFILE_IMAGE_REQUIRED, savedUser.value.signupState)
        assertEquals(SignupState.PROFILE_IMAGE_REQUIRED, response.signupState)
        assertEquals(setOf(ProviderType.EMAIL), savedUser.value.linkedProviders())
        val savedSetting = org.mockito.ArgumentCaptor.forClass(NotificationSetting::class.java)
        verify(notificationSettingRepository).save(savedSetting.capture())
        assertTrue(savedSetting.value.chatMessageEnabled)
        assertTrue(savedSetting.value.recruitmentDeadlineEnabled)
        assertTrue(savedSetting.value.socialActivityEnabled)
        assertTrue(savedSetting.value.marketingEnabled)
    }

    @Test
    fun `만 20세 미만 사용자는 Firebase 회원가입을 할 수 없다`() {
        val identity = FirebaseIdentity("firebase-uid", "user@example.com", ProviderType.EMAIL)
        `when`(firebaseAuthenticationClient.verifyIdToken("id-token")).thenReturn(identity)
        `when`(userAuthIdentityRepository.findByProviderTypeAndProviderUserId(ProviderType.EMAIL, "firebase-uid")).thenReturn(null)
        `when`(userRepository.findByEmail("user@example.com")).thenReturn(null)

        val exception =
            assertThrows(BaseException::class.java) {
                authService.signupWithFirebase(
                    FirebaseSignupRequest(
                        idToken = "id-token",
                        nicknameSelectionToken = "selection-token",
                        nickname = "따스한 사슴 1234",
                        gender = Gender.F,
                        birthDate = LocalDate.now().minusYears(20).plusDays(1),
                    ),
                )
            }

        assertEquals(ErrorCode.MINIMUM_SIGNUP_AGE_NOT_MET, exception.errorCode)
        verifyNoInteractions(nicknameCandidateRepository, notificationSettingRepository)
    }

    @Test
    fun `카카오 액세스 토큰으로 Firebase 커스텀 토큰을 발급한다`() {
        `when`(kakaoClient.getTokenInfo("kakao-token")).thenReturn(KakaoTokenInfoResponse(12345L, 987654L, 3600L))
        `when`(firebaseAuthenticationClient.createKakaoCustomToken("12345")).thenReturn("firebase-custom-token")

        val response = authService.createKakaoCustomToken(KakaoCustomTokenRequest("kakao-token"))

        assertEquals("firebase-custom-token", response.customToken)
    }

    @Test
    fun `다른 카카오 앱에서 발급된 액세스 토큰은 거부한다`() {
        `when`(kakaoClient.getTokenInfo("foreign-token")).thenReturn(KakaoTokenInfoResponse(12345L, 111111L, 3600L))

        val exception =
            assertThrows(BaseException::class.java) {
                authService.createKakaoCustomToken(KakaoCustomTokenRequest("foreign-token"))
            }

        assertEquals(ErrorCode.INVALID_KAKAO_APP, exception.errorCode)
    }

    @Test
    fun `Web 카카오 인가 코드는 액세스 토큰 교환과 앱 검증 후 Firebase 토큰으로 변환한다`() {
        val redirectUri = "https://moyeotrip.github.io/moyeoTrip-Web/auth/kakao/callback"
        `when`(kakaoClient.exchangeAuthorizationCode("authorization-code", redirectUri)).thenReturn("kakao-access-token")
        `when`(kakaoClient.getTokenInfo("kakao-access-token")).thenReturn(KakaoTokenInfoResponse(12345L, 987654L, 3600L))
        `when`(firebaseAuthenticationClient.createKakaoCustomToken("12345")).thenReturn("firebase-custom-token")

        val response = authService.createKakaoCustomToken(KakaoAuthorizationCodeRequest("authorization-code", redirectUri))

        assertEquals("firebase-custom-token", response.customToken)
        verify(kakaoClient).exchangeAuthorizationCode("authorization-code", redirectUri)
    }

    @Test
    fun `허용 목록에 없는 Web 카카오 redirect URI는 교환 전에 거부한다`() {
        val exception =
            assertThrows(BaseException::class.java) {
                authService.createKakaoCustomToken(
                    KakaoAuthorizationCodeRequest("authorization-code", "https://attacker.example/callback"),
                )
            }

        assertEquals(ErrorCode.INVALID_KAKAO_REDIRECT_URI, exception.errorCode)
        verifyNoInteractions(kakaoClient)
    }

    @Test
    fun `Kakao가 인가 코드를 거부하면 안전한 인증 오류로 변환한다`() {
        val redirectUri = "https://moyeotrip.github.io/moyeoTrip-Web/auth/kakao/callback"
        `when`(kakaoClient.exchangeAuthorizationCode("expired-code", redirectUri))
            .thenThrow(HttpClientErrorException(HttpStatus.BAD_REQUEST, "response containing credentials"))

        val exception =
            assertThrows(BaseException::class.java) {
                authService.createKakaoCustomToken(KakaoAuthorizationCodeRequest("expired-code", redirectUri))
            }

        assertEquals(ErrorCode.INVALID_KAKAO_AUTHORIZATION_CODE, exception.errorCode)
        assertEquals(ErrorCode.INVALID_KAKAO_AUTHORIZATION_CODE.errorMessage, exception.message)
    }

    @Test
    fun `로그인 사용자는 Apple 인증 수단을 추가할 수 있다`() {
        val user = User(id = 7L, userRole = UserRole.ROLE_USER)
        val appleIdentity = UserAuthIdentity(user = user, providerType = ProviderType.APPLE, providerUserId = "apple-uid")
        `when`(firebaseAuthenticationClient.verifyIdToken("apple-token"))
            .thenReturn(FirebaseIdentity("apple-uid", "relay@privaterelay.appleid.com", ProviderType.APPLE))
        `when`(userRepository.findById(7L)).thenReturn(java.util.Optional.of(user))
        `when`(userAuthIdentityRepository.findByProviderTypeAndProviderUserId(ProviderType.APPLE, "apple-uid")).thenReturn(null)
        `when`(userAuthIdentityRepository.existsByUserIdAndProviderType(7L, ProviderType.APPLE)).thenReturn(false)
        `when`(userAuthIdentityRepository.findAllByUserId(7L)).thenReturn(listOf(appleIdentity))

        val response = authService.linkFirebaseIdentity(7L, FirebaseLoginRequest("apple-token"))

        assertEquals(setOf(ProviderType.APPLE), response.providers)
        verify(userAuthIdentityRepository).save(any(UserAuthIdentity::class.java))
    }

    @Test
    fun `로그인 사용자는 Google 인증 수단을 추가할 수 있다`() {
        val user = User(id = 7L, userRole = UserRole.ROLE_USER)
        val googleIdentity = UserAuthIdentity(user = user, providerType = ProviderType.GOOGLE, providerUserId = "google-uid")
        `when`(firebaseAuthenticationClient.verifyIdToken("google-token"))
            .thenReturn(FirebaseIdentity("google-uid", "user@gmail.com", ProviderType.GOOGLE))
        `when`(userRepository.findById(7L)).thenReturn(java.util.Optional.of(user))
        `when`(userAuthIdentityRepository.findByProviderTypeAndProviderUserId(ProviderType.GOOGLE, "google-uid"))
            .thenReturn(null)
        `when`(userAuthIdentityRepository.existsByUserIdAndProviderType(7L, ProviderType.GOOGLE)).thenReturn(false)
        `when`(userAuthIdentityRepository.findAllByUserId(7L)).thenReturn(listOf(googleIdentity))

        val response = authService.linkFirebaseIdentity(7L, FirebaseLoginRequest("google-token"))

        assertEquals(setOf(ProviderType.GOOGLE), response.providers)
        verify(userAuthIdentityRepository).save(any(UserAuthIdentity::class.java))
    }

    @Test
    fun `Firebase ID Token으로 변환한 Kakao 인증 수단을 추가할 수 있다`() {
        val user = User(id = 7L, userRole = UserRole.ROLE_USER)
        val kakaoIdentity = UserAuthIdentity(user = user, providerType = ProviderType.KAKAO, providerUserId = "12345")
        `when`(firebaseAuthenticationClient.verifyIdToken("kakao-firebase-token"))
            .thenReturn(FirebaseIdentity("12345", null, ProviderType.KAKAO))
        `when`(userRepository.findById(7L)).thenReturn(java.util.Optional.of(user))
        `when`(userAuthIdentityRepository.findByProviderTypeAndProviderUserId(ProviderType.KAKAO, "12345"))
            .thenReturn(null)
        `when`(userAuthIdentityRepository.existsByUserIdAndProviderType(7L, ProviderType.KAKAO)).thenReturn(false)
        `when`(userAuthIdentityRepository.findAllByUserId(7L)).thenReturn(listOf(kakaoIdentity))

        val response = authService.linkFirebaseIdentity(7L, FirebaseLoginRequest("kakao-firebase-token"))

        assertEquals(setOf(ProviderType.KAKAO), response.providers)
        verify(userAuthIdentityRepository).save(any(UserAuthIdentity::class.java))
    }

    @Test
    fun `추가로 연결한 Apple 인증 수단으로 기존 사용자 로그인에 성공한다`() {
        val user =
            User.createFirebaseUser(
                email = "user@example.com",
                nickname = "모여트립",
                nicknameColor = NicknameColor.BLUE,
                userRole = UserRole.ROLE_USER,
            )
        user.selectProfileImage("user/profile/image/selected.png")
        val appleIdentity = UserAuthIdentity(user = user, providerType = ProviderType.APPLE, providerUserId = "apple-uid")
        `when`(firebaseAuthenticationClient.verifyIdToken("apple-token"))
            .thenReturn(FirebaseIdentity("apple-uid", "relay@privaterelay.appleid.com", ProviderType.APPLE))
        `when`(userAuthIdentityRepository.findByProviderTypeAndProviderUserId(ProviderType.APPLE, "apple-uid"))
            .thenReturn(appleIdentity)
        `when`(jwtUtil.generateAccessToken(0L, "모여트립")).thenReturn("access-token")
        `when`(jwtUtil.generateRotateId()).thenReturn("rotate-id")
        `when`(jwtUtil.generateRefreshToken(0L, "rotate-id")).thenReturn("refresh-token")

        val response = authService.loginWithFirebase(FirebaseLoginRequest("apple-token"))

        assertFalse(response.isNewUser)
        assertEquals(SignupState.SIGNUP_COMPLETE, response.signupState)
        assertEquals("access-token", response.accessToken)
        assertEquals("refresh-token", response.refreshToken)
    }

    @Test
    fun `프로필 이미지를 선택하지 않은 사용자는 재로그인 시 토큰과 진행 상태를 받는다`() {
        val user =
            User.createFirebaseUser(
                email = "user@example.com",
                nickname = "따스한 사슴 1234",
                nicknameColor = NicknameColor.BLUE,
                userRole = UserRole.ROLE_USER,
            )
        val identity = UserAuthIdentity(user = user, providerType = ProviderType.EMAIL, providerUserId = "firebase-uid")
        `when`(firebaseAuthenticationClient.verifyIdToken("id-token"))
            .thenReturn(FirebaseIdentity("firebase-uid", "user@example.com", ProviderType.EMAIL))
        `when`(userAuthIdentityRepository.findByProviderTypeAndProviderUserId(ProviderType.EMAIL, "firebase-uid"))
            .thenReturn(identity)
        `when`(jwtUtil.generateAccessToken(0L, "따스한 사슴 1234")).thenReturn("access-token")
        `when`(jwtUtil.generateRotateId()).thenReturn("rotate-id")
        `when`(jwtUtil.generateRefreshToken(0L, "rotate-id")).thenReturn("refresh-token")

        val response = authService.loginWithFirebase(FirebaseLoginRequest("id-token"))

        assertFalse(response.isNewUser)
        assertEquals(SignupState.PROFILE_IMAGE_REQUIRED, response.signupState)
        assertEquals("access-token", response.accessToken)
        assertEquals("refresh-token", response.refreshToken)
    }

    @Test
    fun `다른 사용자에게 연결된 카카오 인증 수단은 가져올 수 없다`() {
        val currentUser = User(id = 7L, userRole = UserRole.ROLE_USER)
        val owner = User(id = 8L, userRole = UserRole.ROLE_USER)
        val existingIdentity = UserAuthIdentity(user = owner, providerType = ProviderType.KAKAO, providerUserId = "12345")
        `when`(firebaseAuthenticationClient.verifyIdToken("kakao-firebase-token"))
            .thenReturn(FirebaseIdentity("12345", null, ProviderType.KAKAO))
        `when`(userRepository.findById(7L)).thenReturn(java.util.Optional.of(currentUser))
        `when`(userAuthIdentityRepository.findByProviderTypeAndProviderUserId(ProviderType.KAKAO, "12345"))
            .thenReturn(existingIdentity)

        val exception =
            assertThrows(BaseException::class.java) {
                authService.linkFirebaseIdentity(7L, FirebaseLoginRequest("kakao-firebase-token"))
            }

        assertEquals(ErrorCode.AUTH_IDENTITY_ALREADY_LINKED, exception.errorCode)
    }
}
