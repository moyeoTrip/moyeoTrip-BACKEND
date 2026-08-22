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
import kr.hanchae.moyeotrip.controller.auth.response.FirebaseCustomTokenResponse
import kr.hanchae.moyeotrip.controller.auth.response.FirebaseLoginResponse
import kr.hanchae.moyeotrip.controller.auth.response.LinkedProvidersResponse
import kr.hanchae.moyeotrip.controller.auth.response.ServiceTokensResponse
import kr.hanchae.moyeotrip.controller.client.KakaoTokenInfoResponse
import kr.hanchae.moyeotrip.entity.notification.NotificationSetting
import kr.hanchae.moyeotrip.entity.user.NicknameColor
import kr.hanchae.moyeotrip.entity.user.ProviderType
import kr.hanchae.moyeotrip.entity.user.SignupState
import kr.hanchae.moyeotrip.entity.user.User
import kr.hanchae.moyeotrip.entity.user.UserInformation
import kr.hanchae.moyeotrip.entity.user.UserRole
import kr.hanchae.moyeotrip.exception.AlreadyExistNicknameException
import kr.hanchae.moyeotrip.exception.AlreadyExistedProviderUserIdException
import kr.hanchae.moyeotrip.exception.BaseException
import kr.hanchae.moyeotrip.exception.ErrorCode
import kr.hanchae.moyeotrip.exception.InvalidRefreshTokenException
import kr.hanchae.moyeotrip.exception.KakaoClientException
import kr.hanchae.moyeotrip.exception.UserNotFoundException
import kr.hanchae.moyeotrip.repository.NicknameCandidateRepository
import kr.hanchae.moyeotrip.repository.NotificationSettingRepository
import kr.hanchae.moyeotrip.repository.UserAuthIdentityRepository
import kr.hanchae.moyeotrip.repository.UserRepository
import kr.hanchae.moyeotrip.utils.jwt.JwtUtil
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestClientResponseException
import java.net.URI
import java.time.LocalDate

@Service
@Transactional
class AuthService(
    private val userRepository: UserRepository,
    private val jwtUtil: JwtUtil,
    private val firebaseAuthenticationClient: FirebaseAuthenticationClient,
    private val kakaoClient: KakaoClient,
    private val kakaoProperties: KakaoProperties,
    private val userAuthIdentityRepository: UserAuthIdentityRepository,
    private val nicknameCandidateRepository: NicknameCandidateRepository,
    private val notificationSettingRepository: NotificationSettingRepository,
) {
    fun createKakaoCustomToken(request: KakaoCustomTokenRequest): FirebaseCustomTokenResponse = createKakaoCustomToken(request.accessToken)

    fun createKakaoCustomToken(request: KakaoAuthorizationCodeRequest): FirebaseCustomTokenResponse {
        validateKakaoWebConfiguration()
        validateKakaoRedirectUri(request.redirectUri)
        val accessToken = exchangeKakaoAuthorizationCode(request.code, request.redirectUri)
        return createKakaoCustomToken(accessToken)
    }

    private fun createKakaoCustomToken(accessToken: String): FirebaseCustomTokenResponse {
        val tokenInfo = getKakaoTokenInfo(accessToken)
        if (tokenInfo.appId != kakaoProperties.appId) {
            throw BaseException(ErrorCode.INVALID_KAKAO_APP, ErrorCode.INVALID_KAKAO_APP.errorMessage)
        }
        return FirebaseCustomTokenResponse(
            firebaseAuthenticationClient.createKakaoCustomToken(tokenInfo.id.toString()),
        )
    }

    private fun validateKakaoWebConfiguration() {
        if (kakaoProperties.restApiKey.isBlank() || kakaoProperties.allowedRedirectUris.none { it.isNotBlank() }) {
            throw BaseException(ErrorCode.KAKAO_AUTH_UNAVAILABLE, ErrorCode.KAKAO_AUTH_UNAVAILABLE.errorMessage)
        }
    }

    private fun validateKakaoRedirectUri(redirectUri: String) {
        val uri = runCatching { URI.create(redirectUri) }.getOrNull()
        val isLoopback = uri?.host == "localhost" || uri?.host == "127.0.0.1" || uri?.host == "::1"
        val isAllowedScheme = uri?.scheme == "https" || (uri?.scheme == "http" && isLoopback)
        val isCleanAbsoluteUri =
            uri?.isAbsolute == true &&
                isAllowedScheme &&
                uri.host != null &&
                uri.userInfo == null &&
                uri.query == null &&
                uri.fragment == null
        val allowedRedirectUris = kakaoProperties.allowedRedirectUris.map(String::trim)
        if (!isCleanAbsoluteUri || redirectUri !in allowedRedirectUris) {
            throw BaseException(ErrorCode.INVALID_KAKAO_REDIRECT_URI, ErrorCode.INVALID_KAKAO_REDIRECT_URI.errorMessage)
        }
    }

    private fun exchangeKakaoAuthorizationCode(
        code: String,
        redirectUri: String,
    ): String =
        try {
            kakaoClient.exchangeAuthorizationCode(code, redirectUri)
        } catch (exception: RestClientResponseException) {
            val errorCode =
                if (exception.statusCode.is4xxClientError) {
                    ErrorCode.INVALID_KAKAO_AUTHORIZATION_CODE
                } else {
                    ErrorCode.KAKAO_AUTH_UNAVAILABLE
                }
            throw BaseException(errorCode, errorCode.errorMessage)
        } catch (exception: RestClientException) {
            throw BaseException(ErrorCode.KAKAO_AUTH_UNAVAILABLE, ErrorCode.KAKAO_AUTH_UNAVAILABLE.errorMessage)
        } catch (exception: IllegalStateException) {
            throw BaseException(ErrorCode.KAKAO_AUTH_UNAVAILABLE, ErrorCode.KAKAO_AUTH_UNAVAILABLE.errorMessage)
        }

    fun loginWithFirebase(request: FirebaseLoginRequest): FirebaseLoginResponse {
        val identity = firebaseAuthenticationClient.verifyIdToken(request.idToken)
        val user =
            findUser(identity) ?: return FirebaseLoginResponse(
                isNewUser = true,
                signupState = SignupState.USER_INFO_REQUIRED,
                providerType = identity.providerType,
            )

        request.fcmToken
            ?.takeIf { it != user.fcmToken }
            ?.let(user::changeFcmToken)
        user.recordLogin()

        if (user.signupState == SignupState.USER_INFO_REQUIRED) {
            return FirebaseLoginResponse(
                isNewUser = true,
                signupState = SignupState.USER_INFO_REQUIRED,
                providerType = identity.providerType,
            )
        }
        val tokens = makeTokens(user)
        return FirebaseLoginResponse(
            accessToken = tokens.accessToken,
            refreshToken = tokens.refreshToken,
            isNewUser = false,
            signupState = user.signupState,
            providerType = identity.providerType,
        )
    }

    fun signupWithFirebase(request: FirebaseSignupRequest): ServiceTokensResponse {
        val identity = firebaseAuthenticationClient.verifyIdToken(request.idToken)

        val existingUser = findUser(identity)
        if (existingUser != null) {
            if (existingUser.signupState == SignupState.SIGNUP_COMPLETE) {
                throw AlreadyExistedProviderUserIdException()
            }
        } else if (identity.email != null && userRepository.findByEmail(identity.email) != null) {
            throw BaseException(ErrorCode.AUTH_IDENTITY_ALREADY_LINKED, "해당 이메일의 기존 계정에 로그인 수단을 연결해 주세요.")
        }

        validateMinimumSignupAge(request.birthDate)

        val nicknameColor = validateNicknameSelection(request.nicknameSelectionToken, request.nickname)
        val nickname = request.nickname
        if (userRepository.existsByInformationNickname(nickname)) {
            throw AlreadyExistNicknameException()
        }
        if (existingUser != null) {
            existingUser.setSignupInformation(
                UserInformation(
                    nickname = nickname,
                    nicknameColor = nicknameColor,
                    gender = request.gender,
                    birthDate = request.birthDate,
                ),
            )
            request.fcmToken?.let(existingUser::changeFcmToken)
            existingUser.recordLogin()
            return makeTokens(existingUser)
        }

        val user =
            User.createFirebaseUser(
                email = identity.email,
                nickname = nickname,
                nicknameColor = nicknameColor,
                gender = request.gender,
                birthDate = request.birthDate,
                userRole = UserRole.ROLE_USER,
            )
        user.addAuthIdentity(identity.providerType, identity.uid)
        request.fcmToken?.let(user::changeFcmToken)
        user.recordLogin()
        val savedUser = userRepository.save(user)
        notificationSettingRepository.save(NotificationSetting(user = savedUser))
        return makeTokens(savedUser)
    }

    private fun validateMinimumSignupAge(birthDate: LocalDate) {
        if (birthDate.isAfter(LocalDate.now().minusYears(MINIMUM_SIGNUP_AGE.toLong()))) {
            throw BaseException(ErrorCode.MINIMUM_SIGNUP_AGE_NOT_MET)
        }
    }

    fun linkFirebaseIdentity(
        userId: Long,
        request: FirebaseLoginRequest,
    ): LinkedProvidersResponse {
        val identity = firebaseAuthenticationClient.verifyIdToken(request.idToken)
        return linkIdentity(userId, identity.providerType, identity.uid)
    }

    @Transactional(readOnly = true)
    fun getLinkedProviders(userId: Long): LinkedProvidersResponse {
        if (!userRepository.existsById(userId)) {
            throw UserNotFoundException(userId)
        }
        return linkedProvidersResponse(userId)
    }

    private fun linkedProvidersResponse(userId: Long): LinkedProvidersResponse =
        LinkedProvidersResponse(
            userAuthIdentityRepository.findAllByUserId(userId).mapTo(linkedSetOf()) { it.providerType },
        )

    fun refreshTokens(request: RefreshAccessTokenRequest): ServiceTokensResponse {
        val refreshToken = request.refreshToken
        if (!jwtUtil.validateToken(jwtUtil.refreshKey, refreshToken) ||
            !jwtUtil.validateCachedRefreshTokenRotateId(refreshToken)
        ) {
            throw InvalidRefreshTokenException()
        }
        val userId = jwtUtil.getUserId(jwtUtil.refreshKey, refreshToken)
        val user = userRepository.findById(userId).orElseThrow { UserNotFoundException(userId) }
        return makeTokens(user)
    }

    private fun getKakaoTokenInfo(accessToken: String): KakaoTokenInfoResponse =
        try {
            kakaoClient.getTokenInfo(accessToken)
        } catch (exception: RestClientException) {
            throw KakaoClientException(ErrorCode.KAKAO_CLIENT_EXCEPTION.errorMessage)
        } catch (exception: IllegalStateException) {
            throw KakaoClientException(ErrorCode.KAKAO_CLIENT_EXCEPTION.errorMessage)
        }

    private fun findUser(identity: FirebaseIdentity): User? =
        userAuthIdentityRepository.findByProviderTypeAndProviderUserId(identity.providerType, identity.uid)?.user

    private fun validateNicknameSelection(
        selectionToken: String,
        nickname: String,
    ): NicknameColor {
        val candidates = nicknameCandidateRepository.consume(selectionToken)
        val color =
            candidates?.get(nickname) ?: throw BaseException(
                ErrorCode.INVALID_NICKNAME_SELECTION,
                ErrorCode.INVALID_NICKNAME_SELECTION.errorMessage,
            )
        return color
    }

    private fun linkIdentity(
        userId: Long,
        providerType: ProviderType,
        providerUserId: String,
    ): LinkedProvidersResponse {
        val user = userRepository.findById(userId).orElseThrow { UserNotFoundException(userId) }
        val existingIdentity = userAuthIdentityRepository.findByProviderTypeAndProviderUserId(providerType, providerUserId)
        if (existingIdentity != null) {
            if (existingIdentity.user.id != userId) {
                throw BaseException(ErrorCode.AUTH_IDENTITY_ALREADY_LINKED, ErrorCode.AUTH_IDENTITY_ALREADY_LINKED.errorMessage)
            }
            return linkedProvidersResponse(userId)
        }
        if (userAuthIdentityRepository.existsByUserIdAndProviderType(userId, providerType)) {
            throw BaseException(ErrorCode.AUTH_PROVIDER_ALREADY_LINKED, ErrorCode.AUTH_PROVIDER_ALREADY_LINKED.errorMessage)
        }
        userAuthIdentityRepository.save(user.addAuthIdentity(providerType, providerUserId))
        return linkedProvidersResponse(userId)
    }

    private fun makeTokens(user: User): ServiceTokensResponse {
        if (user.signupState == SignupState.USER_INFO_REQUIRED || user.information == null) {
            throw BaseException(ErrorCode.USER_INFO_REQUIRED, ErrorCode.USER_INFO_REQUIRED.errorMessage)
        }
        val accessToken = jwtUtil.generateAccessToken(user.id, user.information!!.nickname)
        val rotateId = jwtUtil.generateRotateId()
        val refreshToken = jwtUtil.generateRefreshToken(user.id, rotateId)
        jwtUtil.storeCachedRefreshTokenRotateId(user.id, rotateId)
        return ServiceTokensResponse(accessToken, refreshToken, user.signupState)
    }

    private companion object {
        const val MINIMUM_SIGNUP_AGE = 20
    }
}
