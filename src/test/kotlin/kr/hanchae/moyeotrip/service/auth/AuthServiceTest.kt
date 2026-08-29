package kr.hanchae.moyeotrip.service.auth

import kr.hanchae.moyeotrip.client.FirebaseAuthenticationClient
import kr.hanchae.moyeotrip.client.FirebaseIdentity
import kr.hanchae.moyeotrip.client.KakaoClient
import kr.hanchae.moyeotrip.config.properties.KakaoProperties
import kr.hanchae.moyeotrip.controller.auth.request.FirebaseLoginRequest
import kr.hanchae.moyeotrip.controller.auth.request.FirebaseSignupRequest
import kr.hanchae.moyeotrip.controller.auth.request.KakaoAuthorizationCodeRequest
import kr.hanchae.moyeotrip.controller.auth.request.KakaoCustomTokenRequest
import kr.hanchae.moyeotrip.controller.auth.request.RefreshAccessTokenRequest
import kr.hanchae.moyeotrip.controller.client.KakaoTokenInfoResponse
import kr.hanchae.moyeotrip.entity.notification.NotificationSetting
import kr.hanchae.moyeotrip.entity.terms.AgreementTerm
import kr.hanchae.moyeotrip.entity.terms.AgreementTermCode
import kr.hanchae.moyeotrip.entity.tour.LegalDongCode
import kr.hanchae.moyeotrip.entity.user.Gender
import kr.hanchae.moyeotrip.entity.user.NicknameColor
import kr.hanchae.moyeotrip.entity.user.ProviderType
import kr.hanchae.moyeotrip.entity.user.SignupState
import kr.hanchae.moyeotrip.entity.user.TravelStyle
import kr.hanchae.moyeotrip.entity.user.User
import kr.hanchae.moyeotrip.entity.user.UserAuthIdentity
import kr.hanchae.moyeotrip.entity.user.UserRole
import kr.hanchae.moyeotrip.exception.BaseException
import kr.hanchae.moyeotrip.exception.ErrorCode
import kr.hanchae.moyeotrip.exception.InvalidRefreshTokenException
import kr.hanchae.moyeotrip.exception.KakaoClientException
import kr.hanchae.moyeotrip.exception.UserNotFoundException
import kr.hanchae.moyeotrip.repository.AgreementTermRepository
import kr.hanchae.moyeotrip.repository.LegalDongCodeRepository
import kr.hanchae.moyeotrip.repository.NicknameCandidateRepository
import kr.hanchae.moyeotrip.repository.NotificationSettingRepository
import kr.hanchae.moyeotrip.repository.TravelStyleRepository
import kr.hanchae.moyeotrip.repository.UserAuthIdentityRepository
import kr.hanchae.moyeotrip.repository.UserRepository
import kr.hanchae.moyeotrip.repository.UserTermsAgreementRepository
import kr.hanchae.moyeotrip.utils.jwt.JwtUtil
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anySet
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.RestClientException
import java.time.LocalDate

class AuthServiceTest {
    private lateinit var userRepository: UserRepository
    private lateinit var jwtUtil: JwtUtil
    private lateinit var firebaseAuthenticationClient: FirebaseAuthenticationClient
    private lateinit var kakaoClient: KakaoClient
    private lateinit var userAuthIdentityRepository: UserAuthIdentityRepository
    private lateinit var nicknameCandidateRepository: NicknameCandidateRepository
    private lateinit var notificationSettingRepository: NotificationSettingRepository
    private lateinit var agreementTermRepository: AgreementTermRepository
    private lateinit var userTermsAgreementRepository: UserTermsAgreementRepository
    private lateinit var travelStyleRepository: TravelStyleRepository
    private lateinit var legalDongCodeRepository: LegalDongCodeRepository
    private lateinit var userService: UserService
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
        agreementTermRepository = mock(AgreementTermRepository::class.java)
        userTermsAgreementRepository = mock(UserTermsAgreementRepository::class.java)
        travelStyleRepository = mock(TravelStyleRepository::class.java)
        legalDongCodeRepository = mock(LegalDongCodeRepository::class.java)
        userService = mock(UserService::class.java)
        `when`(travelStyleRepository.findAllById(setOf(1L))).thenReturn(listOf(TravelStyle(id = 1L, label = "자연")))
        `when`(legalDongCodeRepository.findAllById(setOf(1L)))
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
                    allowedRedirectUris = listOf("https://moyeo-trip.jayden-bin.cc/moyeoTrip-Web/auth/kakao/callback"),
                ),
                userAuthIdentityRepository,
                nicknameCandidateRepository,
                notificationSettingRepository,
                agreementTermRepository,
                userTermsAgreementRepository,
                travelStyleRepository,
                legalDongCodeRepository,
                userService,
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
        val previousTokenOwner =
            User(id = 9L, userRole = UserRole.ROLE_USER).also {
                it.changeFcmToken("fcm-token")
            }
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
        `when`(agreementTermRepository.findAllByActiveTrueOrderByIdAsc()).thenReturn(requiredTerms())
        `when`(userRepository.findByFcmToken("fcm-token")).thenReturn(previousTokenOwner)
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
                    travelStyleIds = setOf(1L),
                    interestedRegionIds = setOf(1L),
                    fcmToken = "fcm-token",
                    agreedTermIds = setOf(1L, 2L),
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
        assertEquals(
            setOf(1L),
            savedUser.value.travelStyles
                .map { it.id }
                .toSet(),
        )
        assertEquals(
            setOf(1L),
            savedUser.value.interestedRegions
                .map { it.id }
                .toSet(),
        )
        assertNull(previousTokenOwner.fcmToken)
        verify(userRepository).flush()
        assertEquals(setOf(ProviderType.EMAIL), savedUser.value.linkedProviders())
        val savedSetting = org.mockito.ArgumentCaptor.forClass(NotificationSetting::class.java)
        verify(notificationSettingRepository).save(savedSetting.capture())
        assertTrue(savedSetting.value.chatMessageEnabled)
        assertTrue(savedSetting.value.recruitmentDeadlineEnabled)
        assertTrue(savedSetting.value.socialActivityEnabled)
        assertFalse(savedSetting.value.marketingEnabled)
    }

    @Test
    fun `마케팅 약관에 동의하면 마케팅 알림을 기본 활성화한다`() {
        val identity = FirebaseIdentity("firebase-uid", "user@example.com", ProviderType.EMAIL)
        `when`(firebaseAuthenticationClient.verifyIdToken("id-token")).thenReturn(identity)
        `when`(userAuthIdentityRepository.findByProviderTypeAndProviderUserId(ProviderType.EMAIL, "firebase-uid")).thenReturn(null)
        `when`(userRepository.findByEmail("user@example.com")).thenReturn(null)
        `when`(agreementTermRepository.findAllByActiveTrueOrderByIdAsc()).thenReturn(requiredTerms())
        `when`(nicknameCandidateRepository.consume("selection-token")).thenReturn(mapOf("따스한 사슴 1234" to NicknameColor.RED))
        `when`(userRepository.existsByInformationNickname("따스한 사슴 1234")).thenReturn(false)
        `when`(userRepository.save(any(User::class.java))).thenAnswer { it.arguments[0] as User }
        `when`(jwtUtil.generateAccessToken(0L, "따스한 사슴 1234")).thenReturn("access-token")
        `when`(jwtUtil.generateRotateId()).thenReturn("rotate-id")
        `when`(jwtUtil.generateRefreshToken(0L, "rotate-id")).thenReturn("refresh-token")

        authService.signupWithFirebase(
            FirebaseSignupRequest(
                idToken = "id-token",
                nicknameSelectionToken = "selection-token",
                nickname = "따스한 사슴 1234",
                gender = Gender.F,
                birthDate = LocalDate.now().minusYears(20),
                travelStyleIds = setOf(1L),
                interestedRegionIds = setOf(1L),
                agreedTermIds = setOf(1L, 2L, 3L),
            ),
        )

        val savedSetting = org.mockito.ArgumentCaptor.forClass(NotificationSetting::class.java)
        verify(notificationSettingRepository).save(savedSetting.capture())
        assertTrue(savedSetting.value.marketingEnabled)
    }

    @Test
    fun `필수 약관에 동의하지 않으면 Firebase 회원가입을 할 수 없다`() {
        val identity = FirebaseIdentity("firebase-uid", "user@example.com", ProviderType.EMAIL)
        `when`(firebaseAuthenticationClient.verifyIdToken("id-token")).thenReturn(identity)
        `when`(userAuthIdentityRepository.findByProviderTypeAndProviderUserId(ProviderType.EMAIL, "firebase-uid")).thenReturn(null)
        `when`(userRepository.findByEmail("user@example.com")).thenReturn(null)
        `when`(agreementTermRepository.findAllByActiveTrueOrderByIdAsc()).thenReturn(requiredTerms())

        val exception =
            assertThrows(BaseException::class.java) {
                authService.signupWithFirebase(
                    FirebaseSignupRequest(
                        idToken = "id-token",
                        nicknameSelectionToken = "selection-token",
                        nickname = "따스한 사슴 1234",
                        gender = Gender.F,
                        birthDate = LocalDate.now().minusYears(20),
                        travelStyleIds = setOf(1L),
                        interestedRegionIds = setOf(1L),
                        agreedTermIds = setOf(1L),
                    ),
                )
            }

        assertEquals(ErrorCode.REQUIRED_TERMS_NOT_AGREED, exception.errorCode)
        verifyNoInteractions(nicknameCandidateRepository, notificationSettingRepository, userTermsAgreementRepository)
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
                        travelStyleIds = setOf(1L),
                        interestedRegionIds = setOf(1L),
                    ),
                )
            }

        assertEquals(ErrorCode.MINIMUM_SIGNUP_AGE_NOT_MET, exception.errorCode)
        verifyNoInteractions(
            nicknameCandidateRepository,
            notificationSettingRepository,
            agreementTermRepository,
            userTermsAgreementRepository,
        )
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
        val redirectUri = "https://moyeo-trip.jayden-bin.cc/moyeoTrip-Web/auth/kakao/callback"
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
        val redirectUri = "https://moyeo-trip.jayden-bin.cc/moyeoTrip-Web/auth/kakao/callback"
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
    fun `탈퇴 후 30일 이내 로그인으로 복구되면 복구 여부와 새 토큰을 반환한다`() {
        val user =
            User.createFirebaseUser(
                email = "restored@example.com",
                nickname = "돌아온 여행자",
                nicknameColor = NicknameColor.MINT,
                userRole = UserRole.ROLE_USER,
            )
        user.selectProfileImage("profile.webp")
        val identity = UserAuthIdentity(user = user, providerType = ProviderType.EMAIL, providerUserId = "restored-uid")
        `when`(firebaseAuthenticationClient.verifyIdToken("restore-token"))
            .thenReturn(FirebaseIdentity("restored-uid", "restored@example.com", ProviderType.EMAIL))
        `when`(userAuthIdentityRepository.findByProviderTypeAndProviderUserId(ProviderType.EMAIL, "restored-uid"))
            .thenReturn(identity)
        `when`(userService.handleWithdrawnLogin(user)).thenReturn(WithdrawnLoginResult.RESTORED)
        `when`(jwtUtil.generateAccessToken(0L, "돌아온 여행자")).thenReturn("restored-access")
        `when`(jwtUtil.generateRotateId()).thenReturn("restored-rotate")
        `when`(jwtUtil.generateRefreshToken(0L, "restored-rotate")).thenReturn("restored-refresh")

        val response = authService.loginWithFirebase(FirebaseLoginRequest("restore-token"))

        assertTrue(response.reactivated)
        assertFalse(response.isNewUser)
        assertEquals("restored-access", response.accessToken)
        assertEquals("restored-refresh", response.refreshToken)
    }

    @Test
    fun `복구 기간이 지난 탈퇴 계정은 영구 삭제하고 신규 가입 대상으로 응답한다`() {
        val user =
            User.createFirebaseUser(
                email = "expired@example.com",
                nickname = "만료된 여행자",
                nicknameColor = NicknameColor.NAVY,
                userRole = UserRole.ROLE_USER,
            )
        val identity = UserAuthIdentity(user = user, providerType = ProviderType.EMAIL, providerUserId = "expired-uid")
        `when`(firebaseAuthenticationClient.verifyIdToken("expired-token"))
            .thenReturn(FirebaseIdentity("expired-uid", "expired@example.com", ProviderType.EMAIL))
        `when`(userAuthIdentityRepository.findByProviderTypeAndProviderUserId(ProviderType.EMAIL, "expired-uid"))
            .thenReturn(identity)
        `when`(userService.handleWithdrawnLogin(user)).thenReturn(WithdrawnLoginResult.EXPIRED_DELETED)

        val response = authService.loginWithFirebase(FirebaseLoginRequest("expired-token"))

        assertTrue(response.isNewUser)
        assertFalse(response.reactivated)
        assertEquals(SignupState.USER_INFO_REQUIRED, response.signupState)
        assertEquals(null, response.accessToken)
        assertEquals(null, response.refreshToken)
        verifyNoInteractions(jwtUtil)
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

    @Test
    fun `카카오 Web 설정이 비어 있으면 인가 코드 교환을 시작하지 않는다`() {
        val unavailableService =
            AuthService(
                userRepository,
                jwtUtil,
                firebaseAuthenticationClient,
                kakaoClient,
                KakaoProperties(appId = 987654L, restApiKey = "", clientSecret = "", allowedRedirectUris = emptyList()),
                userAuthIdentityRepository,
                nicknameCandidateRepository,
                notificationSettingRepository,
                agreementTermRepository,
                userTermsAgreementRepository,
                travelStyleRepository,
                legalDongCodeRepository,
                userService,
            )

        val exception =
            assertThrows(BaseException::class.java) {
                unavailableService.createKakaoCustomToken(
                    KakaoAuthorizationCodeRequest("code", "https://moyeo-trip.jayden-bin.cc/moyeoTrip-Web/auth/kakao/callback"),
                )
            }

        assertEquals(ErrorCode.KAKAO_AUTH_UNAVAILABLE, exception.errorCode)
        verifyNoInteractions(kakaoClient)
    }

    @Test
    fun `카카오 서버 오류는 인증 서비스 이용 불가로 변환한다`() {
        val redirectUri = "https://moyeo-trip.jayden-bin.cc/moyeoTrip-Web/auth/kakao/callback"
        `when`(kakaoClient.exchangeAuthorizationCode("code", redirectUri))
            .thenThrow(HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR))

        val exception =
            assertThrows(BaseException::class.java) {
                authService.createKakaoCustomToken(KakaoAuthorizationCodeRequest("code", redirectUri))
            }

        assertEquals(ErrorCode.KAKAO_AUTH_UNAVAILABLE, exception.errorCode)
    }

    @Test
    fun `카카오 인가 코드 통신 실패도 인증 서비스 이용 불가로 변환한다`() {
        val redirectUri = "https://moyeo-trip.jayden-bin.cc/moyeoTrip-Web/auth/kakao/callback"
        `when`(kakaoClient.exchangeAuthorizationCode("code", redirectUri)).thenThrow(RestClientException("network"))

        val exception =
            assertThrows(BaseException::class.java) {
                authService.createKakaoCustomToken(KakaoAuthorizationCodeRequest("code", redirectUri))
            }

        assertEquals(ErrorCode.KAKAO_AUTH_UNAVAILABLE, exception.errorCode)
    }

    @Test
    fun `카카오 토큰 정보 조회 실패는 카카오 클라이언트 예외로 변환한다`() {
        `when`(kakaoClient.getTokenInfo("token")).thenThrow(IllegalStateException("invalid response"))

        assertThrows(KakaoClientException::class.java) {
            authService.createKakaoCustomToken(KakaoCustomTokenRequest("token"))
        }
    }

    @Test
    fun `가입 정보가 미완성인 기존 사용자는 신규 가입 단계로 응답한다`() {
        val user = User(id = 7L, userRole = UserRole.ROLE_USER)
        val previousTokenOwner =
            User(id = 9L, userRole = UserRole.ROLE_USER).also {
                it.changeFcmToken("new-fcm")
            }
        val identity = UserAuthIdentity(user = user, providerType = ProviderType.EMAIL, providerUserId = "uid")
        `when`(firebaseAuthenticationClient.verifyIdToken("token"))
            .thenReturn(FirebaseIdentity("uid", "user@example.com", ProviderType.EMAIL))
        `when`(userAuthIdentityRepository.findByProviderTypeAndProviderUserId(ProviderType.EMAIL, "uid"))
            .thenReturn(identity)
        `when`(userRepository.findByFcmToken("new-fcm")).thenReturn(previousTokenOwner)

        val response = authService.loginWithFirebase(FirebaseLoginRequest("token", "new-fcm"))

        assertTrue(response.isNewUser)
        assertEquals(SignupState.USER_INFO_REQUIRED, response.signupState)
        assertEquals("new-fcm", user.fcmToken)
        assertNull(previousTokenOwner.fcmToken)
        verify(userRepository).flush()
    }

    @Test
    fun `가입 완료된 인증 수단으로 다시 회원가입할 수 없다`() {
        val user =
            User.createFirebaseUser(
                email = "user@example.com",
                nickname = "기존 사용자",
                nicknameColor = NicknameColor.BLUE,
                userRole = UserRole.ROLE_USER,
            )
        user.selectProfileImage("profile.png")
        `when`(firebaseAuthenticationClient.verifyIdToken("id-token"))
            .thenReturn(FirebaseIdentity("uid", "user@example.com", ProviderType.EMAIL))
        `when`(userAuthIdentityRepository.findByProviderTypeAndProviderUserId(ProviderType.EMAIL, "uid"))
            .thenReturn(UserAuthIdentity(user = user, providerType = ProviderType.EMAIL, providerUserId = "uid"))

        assertThrows(kr.hanchae.moyeotrip.exception.AlreadyExistedProviderUserIdException::class.java) {
            authService.signupWithFirebase(signupRequest())
        }
    }

    @Test
    fun `동일 이메일 계정이 있으면 신규 인증 수단 회원가입을 거부한다`() {
        `when`(firebaseAuthenticationClient.verifyIdToken("id-token"))
            .thenReturn(FirebaseIdentity("uid", "user@example.com", ProviderType.GOOGLE))
        `when`(userAuthIdentityRepository.findByProviderTypeAndProviderUserId(ProviderType.GOOGLE, "uid")).thenReturn(null)
        `when`(userRepository.findByEmail("user@example.com")).thenReturn(User(id = 9L, userRole = UserRole.ROLE_USER))

        val exception = assertThrows(BaseException::class.java) { authService.signupWithFirebase(signupRequest()) }

        assertEquals(ErrorCode.AUTH_IDENTITY_ALREADY_LINKED, exception.errorCode)
    }

    @Test
    fun `존재하지 않는 약관 ID에는 동의할 수 없다`() {
        stubNewSignupIdentity()
        `when`(agreementTermRepository.findAllByActiveTrueOrderByIdAsc()).thenReturn(requiredTerms())

        val exception =
            assertThrows(BaseException::class.java) {
                authService.signupWithFirebase(signupRequest(agreedTermIds = setOf(1L, 2L, 999L)))
            }

        assertEquals(ErrorCode.INVALID_TERMS_AGREEMENT, exception.errorCode)
    }

    @Test
    fun `후보에 없는 닉네임으로 가입할 수 없다`() {
        stubNewSignupIdentity()
        `when`(agreementTermRepository.findAllByActiveTrueOrderByIdAsc()).thenReturn(requiredTerms())
        `when`(nicknameCandidateRepository.consume("selection-token")).thenReturn(emptyMap())

        val exception = assertThrows(BaseException::class.java) { authService.signupWithFirebase(signupRequest()) }

        assertEquals(ErrorCode.INVALID_NICKNAME_SELECTION, exception.errorCode)
    }

    @Test
    fun `이미 사용 중인 닉네임으로 가입할 수 없다`() {
        stubNewSignupIdentity()
        `when`(agreementTermRepository.findAllByActiveTrueOrderByIdAsc()).thenReturn(requiredTerms())
        `when`(nicknameCandidateRepository.consume("selection-token")).thenReturn(mapOf("따스한 사슴 1234" to NicknameColor.RED))
        `when`(userRepository.existsByInformationNickname("따스한 사슴 1234")).thenReturn(true)

        assertThrows(kr.hanchae.moyeotrip.exception.AlreadyExistNicknameException::class.java) {
            authService.signupWithFirebase(signupRequest())
        }
    }

    @Test
    fun `존재하지 않는 여행 스타일로 회원가입할 수 없다`() {
        stubNewSignupIdentity()
        `when`(agreementTermRepository.findAllByActiveTrueOrderByIdAsc()).thenReturn(requiredTerms())
        `when`(travelStyleRepository.findAllById(setOf(999L))).thenReturn(emptyList())

        val exception =
            assertThrows(BaseException::class.java) {
                authService.signupWithFirebase(signupRequest(travelStyleIds = setOf(999L)))
            }

        assertEquals(ErrorCode.INVALID_TRAVEL_STYLE_SELECTION, exception.errorCode)
    }

    @Test
    fun `경상북도 관심 지역이 아니거나 존재하지 않는 지역으로 회원가입할 수 없다`() {
        stubNewSignupIdentity()
        `when`(agreementTermRepository.findAllByActiveTrueOrderByIdAsc()).thenReturn(requiredTerms())
        `when`(legalDongCodeRepository.findAllById(setOf(999L))).thenReturn(emptyList())

        val exception =
            assertThrows(BaseException::class.java) {
                authService.signupWithFirebase(signupRequest(interestedRegionIds = setOf(999L)))
            }

        assertEquals(ErrorCode.INVALID_INTERESTED_REGION_SELECTION, exception.errorCode)
    }

    @Test
    fun `미완성 기존 사용자의 가입을 완료하며 누락 약관과 알림 설정을 저장한다`() {
        val user = User(id = 7L, userRole = UserRole.ROLE_USER)
        `when`(firebaseAuthenticationClient.verifyIdToken("id-token"))
            .thenReturn(FirebaseIdentity("uid", "user@example.com", ProviderType.EMAIL))
        `when`(userAuthIdentityRepository.findByProviderTypeAndProviderUserId(ProviderType.EMAIL, "uid"))
            .thenReturn(UserAuthIdentity(user = user, providerType = ProviderType.EMAIL, providerUserId = "uid"))
        `when`(agreementTermRepository.findAllByActiveTrueOrderByIdAsc()).thenReturn(requiredTerms())
        `when`(nicknameCandidateRepository.consume("selection-token")).thenReturn(mapOf("따스한 사슴 1234" to NicknameColor.RED))
        `when`(userRepository.existsByInformationNickname("따스한 사슴 1234")).thenReturn(false)
        `when`(userTermsAgreementRepository.findAllByUserIdAndAgreementTermIdIn(org.mockito.ArgumentMatchers.eq(7L), anySet()))
            .thenReturn(emptyList())
        `when`(notificationSettingRepository.findByUserId(7L)).thenReturn(null)
        `when`(jwtUtil.generateAccessToken(7L, "따스한 사슴 1234")).thenReturn("access")
        `when`(jwtUtil.generateRotateId()).thenReturn("rotate")
        `when`(jwtUtil.generateRefreshToken(7L, "rotate")).thenReturn("refresh")

        val response =
            authService.signupWithFirebase(
                signupRequest(
                    agreedTermIds = setOf(1L, 2L, 3L),
                    travelStyleIds = null,
                    interestedRegionIds = null,
                ),
            )

        assertEquals("access", response.accessToken)
        assertTrue(user.travelStyles.isEmpty())
        assertTrue(user.interestedRegions.isEmpty())
        verify(userTermsAgreementRepository).saveAll(org.mockito.ArgumentMatchers.anyList())
        verify(notificationSettingRepository).save(any(NotificationSetting::class.java))
    }

    @Test
    fun `유효하지 않은 refresh token은 재발급할 수 없다`() {
        `when`(jwtUtil.validateToken(jwtUtil.refreshKey, "refresh")).thenReturn(false)

        assertThrows(InvalidRefreshTokenException::class.java) {
            authService.refreshTokens(RefreshAccessTokenRequest("refresh"))
        }
    }

    @Test
    fun `유효한 refresh token으로 토큰을 재발급한다`() {
        val user =
            User.createFirebaseUser(
                email = "user@example.com",
                nickname = "모여트립",
                nicknameColor = NicknameColor.BLUE,
                userRole = UserRole.ROLE_USER,
            )
        `when`(jwtUtil.validateToken(jwtUtil.refreshKey, "refresh")).thenReturn(true)
        `when`(jwtUtil.validateCachedRefreshTokenRotateId("refresh")).thenReturn(true)
        `when`(jwtUtil.getUserId(jwtUtil.refreshKey, "refresh")).thenReturn(7L)
        `when`(userRepository.findById(7L)).thenReturn(java.util.Optional.of(user))
        `when`(jwtUtil.generateAccessToken(0L, "모여트립")).thenReturn("new-access")
        `when`(jwtUtil.generateRotateId()).thenReturn("new-rotate")
        `when`(jwtUtil.generateRefreshToken(0L, "new-rotate")).thenReturn("new-refresh")

        val response = authService.refreshTokens(RefreshAccessTokenRequest("refresh"))

        assertEquals("new-access", response.accessToken)
        assertEquals("new-refresh", response.refreshToken)
    }

    @Test
    fun `존재하지 않는 사용자의 연결 제공자는 조회할 수 없다`() {
        `when`(userRepository.existsById(77L)).thenReturn(false)

        assertThrows(UserNotFoundException::class.java) { authService.getLinkedProviders(77L) }
    }

    @Test
    fun `이미 같은 사용자에게 연결된 인증 수단은 그대로 반환한다`() {
        val user = User(id = 7L, userRole = UserRole.ROLE_USER)
        val identity = UserAuthIdentity(user = user, providerType = ProviderType.GOOGLE, providerUserId = "google")
        `when`(firebaseAuthenticationClient.verifyIdToken("token"))
            .thenReturn(FirebaseIdentity("google", "user@gmail.com", ProviderType.GOOGLE))
        `when`(userRepository.findById(7L)).thenReturn(java.util.Optional.of(user))
        `when`(userAuthIdentityRepository.findByProviderTypeAndProviderUserId(ProviderType.GOOGLE, "google"))
            .thenReturn(identity)
        `when`(userAuthIdentityRepository.findAllByUserId(7L)).thenReturn(listOf(identity))

        val response = authService.linkFirebaseIdentity(7L, FirebaseLoginRequest("token"))

        assertEquals(setOf(ProviderType.GOOGLE), response.providers)
    }

    @Test
    fun `한 사용자에게 같은 제공자의 다른 인증 수단을 중복 연결할 수 없다`() {
        val user = User(id = 7L, userRole = UserRole.ROLE_USER)
        `when`(firebaseAuthenticationClient.verifyIdToken("token"))
            .thenReturn(FirebaseIdentity("new-google", "user@gmail.com", ProviderType.GOOGLE))
        `when`(userRepository.findById(7L)).thenReturn(java.util.Optional.of(user))
        `when`(userAuthIdentityRepository.findByProviderTypeAndProviderUserId(ProviderType.GOOGLE, "new-google"))
            .thenReturn(null)
        `when`(userAuthIdentityRepository.existsByUserIdAndProviderType(7L, ProviderType.GOOGLE)).thenReturn(true)

        val exception =
            assertThrows(BaseException::class.java) {
                authService.linkFirebaseIdentity(7L, FirebaseLoginRequest("token"))
            }

        assertEquals(ErrorCode.AUTH_PROVIDER_ALREADY_LINKED, exception.errorCode)
    }

    private fun stubNewSignupIdentity() {
        `when`(firebaseAuthenticationClient.verifyIdToken("id-token"))
            .thenReturn(FirebaseIdentity("uid", "user@example.com", ProviderType.EMAIL))
        `when`(userAuthIdentityRepository.findByProviderTypeAndProviderUserId(ProviderType.EMAIL, "uid")).thenReturn(null)
        `when`(userRepository.findByEmail("user@example.com")).thenReturn(null)
    }

    private fun signupRequest(
        agreedTermIds: Set<Long> = setOf(1L, 2L),
        travelStyleIds: Set<Long>? = setOf(1L),
        interestedRegionIds: Set<Long>? = setOf(1L),
    ): FirebaseSignupRequest =
        FirebaseSignupRequest(
            idToken = "id-token",
            nicknameSelectionToken = "selection-token",
            nickname = "따스한 사슴 1234",
            gender = Gender.F,
            birthDate = LocalDate.now().minusYears(20),
            travelStyleIds = travelStyleIds,
            interestedRegionIds = interestedRegionIds,
            agreedTermIds = agreedTermIds,
        )

    private fun requiredTerms(): List<AgreementTerm> =
        listOf(
            AgreementTerm(
                id = 1L,
                code = AgreementTermCode.SERVICE,
                title = "[필수] 모여트립 이용약관",
                required = true,
                content = "# 이용약관",
                version = "2026.08.23",
            ),
            AgreementTerm(
                id = 2L,
                code = AgreementTermCode.PRIVACY_COLLECTION,
                title = "[필수] 개인정보 수집 및 이용 동의",
                required = true,
                content = "# 개인정보 수집 및 이용 동의",
                version = "2026.08.23",
            ),
            AgreementTerm(
                id = 3L,
                code = AgreementTermCode.MARKETING,
                title = "[선택] 마케팅 정보 수신 동의",
                required = false,
                content = "# 마케팅 정보 수신 동의",
                version = "2026.08.23",
            ),
        )
}
