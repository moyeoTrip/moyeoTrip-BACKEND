package kr.hanchae.moyeotrip.service.auth

import kr.hanchae.moyeotrip.client.FirebaseAuthenticationClient
import kr.hanchae.moyeotrip.client.FirebaseIdentity
import kr.hanchae.moyeotrip.client.KakaoClient
import kr.hanchae.moyeotrip.config.properties.KakaoProperties
import kr.hanchae.moyeotrip.controller.auth.request.FirebaseLoginRequest
import kr.hanchae.moyeotrip.controller.auth.request.FirebaseSignupRequest
import kr.hanchae.moyeotrip.controller.auth.request.KakaoCustomTokenRequest
import kr.hanchae.moyeotrip.controller.auth.request.RefreshAccessTokenRequest
import kr.hanchae.moyeotrip.controller.auth.response.FirebaseCustomTokenResponse
import kr.hanchae.moyeotrip.controller.auth.response.FirebaseLoginResponse
import kr.hanchae.moyeotrip.controller.auth.response.LinkedProvidersResponse
import kr.hanchae.moyeotrip.controller.auth.response.ServiceTokensResponse
import kr.hanchae.moyeotrip.controller.client.KakaoTokenInfoResponse
import kr.hanchae.moyeotrip.entity.user.Gender
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
import kr.hanchae.moyeotrip.repository.UserAuthIdentityRepository
import kr.hanchae.moyeotrip.repository.UserRepository
import kr.hanchae.moyeotrip.utils.jwt.JwtUtil
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.client.RestClientException

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
) {
    fun createKakaoCustomToken(request: KakaoCustomTokenRequest): FirebaseCustomTokenResponse {
        val tokenInfo = getKakaoTokenInfo(request.accessToken)
        if (tokenInfo.appId != kakaoProperties.appId) {
            throw BaseException(ErrorCode.INVALID_KAKAO_APP, ErrorCode.INVALID_KAKAO_APP.errorMessage)
        }
        return FirebaseCustomTokenResponse(
            firebaseAuthenticationClient.createKakaoCustomToken(tokenInfo.id.toString()),
        )
    }

    fun loginWithFirebase(
        request: FirebaseLoginRequest,
        expectedProvider: ProviderType? = null,
    ): FirebaseLoginResponse {
        val identity = firebaseAuthenticationClient.verifyIdToken(request.idToken)
        validateExpectedProvider(identity, expectedProvider)
        val user = findUser(identity)
        if (user == null) {
            return FirebaseLoginResponse(isNewUser = true, providerType = identity.providerType)
        }

        request.fcmToken
            ?.takeIf { it != user.fcmToken }
            ?.let(user::changeFcmToken)

        if (user.signupState == SignupState.USER_INFO_REQUIRED) {
            return FirebaseLoginResponse(isNewUser = true, providerType = identity.providerType)
        }
        val tokens = makeTokens(user)
        return FirebaseLoginResponse(
            accessToken = tokens.accessToken,
            refreshToken = tokens.refreshToken,
            isNewUser = false,
            providerType = identity.providerType,
        )
    }

    fun signupWithFirebase(
        request: FirebaseSignupRequest,
        expectedProvider: ProviderType? = null,
    ): ServiceTokensResponse {
        val identity = firebaseAuthenticationClient.verifyIdToken(request.idToken)
        validateExpectedProvider(identity, expectedProvider)

        val existingUser = findUser(identity)
        if (existingUser != null) {
            if (existingUser.signupState == SignupState.SIGNUP_COMPLETE) {
                throw AlreadyExistedProviderUserIdException()
            }
        } else if (identity.email != null && userRepository.findByEmail(identity.email) != null) {
            throw BaseException(ErrorCode.AUTH_IDENTITY_ALREADY_LINKED, "해당 이메일의 기존 계정에 로그인 수단을 연결해 주세요.")
        }

        val nicknameColor = validateNicknameSelection(request.nicknameSelectionToken, request.nickname)
        val nickname = request.nickname
        if (userRepository.existsByInformationNickname(nickname)) {
            throw AlreadyExistNicknameException()
        }
        if (existingUser != null) {
            existingUser.changeSignupStateComplete(
                UserInformation(nickname = nickname, nicknameColor = nicknameColor, gender = Gender.N),
            )
            request.fcmToken?.let(existingUser::changeFcmToken)
            return makeTokens(existingUser)
        }

        val user =
            User.createFirebaseUser(
                email = identity.email,
                nickname = nickname,
                nicknameColor = nicknameColor,
                userRole = UserRole.ROLE_USER,
            )
        user.addAuthIdentity(identity.providerType, identity.uid)
        request.fcmToken?.let(user::changeFcmToken)
        return makeTokens(userRepository.save(user))
    }

    fun linkFirebaseIdentity(
        userId: Long,
        request: FirebaseLoginRequest,
    ): LinkedProvidersResponse {
        val identity = firebaseAuthenticationClient.verifyIdToken(request.idToken)
        return linkIdentity(userId, identity.providerType, identity.uid)
    }

    fun linkKakaoIdentity(
        userId: Long,
        request: KakaoCustomTokenRequest,
    ): LinkedProvidersResponse {
        val tokenInfo = getKakaoTokenInfo(request.accessToken)
        if (tokenInfo.appId != kakaoProperties.appId) {
            throw BaseException(ErrorCode.INVALID_KAKAO_APP, ErrorCode.INVALID_KAKAO_APP.errorMessage)
        }
        return linkIdentity(userId, ProviderType.KAKAO, tokenInfo.id.toString())
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
            throw KakaoClientException(exception.message)
        } catch (exception: IllegalStateException) {
            throw KakaoClientException(exception.message)
        }

    private fun validateExpectedProvider(
        identity: FirebaseIdentity,
        expectedProvider: ProviderType?,
    ) {
        if (expectedProvider != null && identity.providerType != expectedProvider) {
            throw BaseException(ErrorCode.INVALID_AUTH_PROVIDER, ErrorCode.INVALID_AUTH_PROVIDER.errorMessage)
        }
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
        return ServiceTokensResponse(accessToken, refreshToken)
    }
}
