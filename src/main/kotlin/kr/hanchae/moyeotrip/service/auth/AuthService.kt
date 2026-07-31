package kr.hanchae.moyeotrip.service.auth

import kr.hanchae.moyeotrip.config.security.CustomUserDto
import kr.hanchae.moyeotrip.controller.auth.request.EmailLoginRequest
import kr.hanchae.moyeotrip.controller.auth.request.EmailSignupRequest
import kr.hanchae.moyeotrip.controller.auth.request.KakaoLoginRequest
import kr.hanchae.moyeotrip.controller.auth.request.UserCreateRequest
import kr.hanchae.moyeotrip.controller.auth.request.RefreshAccessTokenRequest
import kr.hanchae.moyeotrip.exception.InvalidRefreshTokenException
import kr.hanchae.moyeotrip.exception.UserNotFoundException
import kr.hanchae.moyeotrip.controller.auth.response.KakaoLoginResponse
import kr.hanchae.moyeotrip.controller.auth.response.NicknameCheckResponse
import kr.hanchae.moyeotrip.controller.auth.response.ServiceTokensResponse
import kr.hanchae.moyeotrip.controller.client.KakaoUserInfoResponse
import kr.hanchae.moyeotrip.entity.user.ProviderType
import kr.hanchae.moyeotrip.entity.user.SignupState
import kr.hanchae.moyeotrip.entity.user.User
import kr.hanchae.moyeotrip.entity.user.User.Companion.createEmailUser
import kr.hanchae.moyeotrip.entity.user.UserRole
import kr.hanchae.moyeotrip.exception.AlreadyExistNicknameException
import kr.hanchae.moyeotrip.exception.AlreadyExistedProviderUserIdException
import kr.hanchae.moyeotrip.exception.BaseException
import kr.hanchae.moyeotrip.exception.ErrorCode
import kr.hanchae.moyeotrip.exception.KakaoClientException
import kr.hanchae.moyeotrip.repository.ObjectStorageRepository
import kr.hanchae.moyeotrip.repository.UserRepository
import kr.hanchae.moyeotrip.utils.jwt.JwtUtil
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.crypto.password.PasswordEncoder

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class AuthService(
    private val userRepository: UserRepository,
    private val jwtUtil: JwtUtil,
    private val objectStorageRepository: ObjectStorageRepository,
    private val authenticationManager: AuthenticationManager,
    private val passwordEncoder: PasswordEncoder,
) {
    fun refreshTokens(request: RefreshAccessTokenRequest): ServiceTokensResponse {
        val refreshToken = request.refreshToken
        require(
            jwtUtil.validateToken(jwtUtil.refreshKey, refreshToken) &&
                    jwtUtil.validateCachedRefreshTokenRotateId(refreshToken),
        ) {
            throw InvalidRefreshTokenException()
        }
        val userId = jwtUtil.getUserId(jwtUtil.refreshKey, refreshToken)
        val user = userRepository.findById(userId).orElseThrow { UserNotFoundException(userId) }
        return makeTokens(user)
    }

    private fun makeTokens(user: User): ServiceTokensResponse {
        if (user.signupState == SignupState.USER_INFO_REQUIRED) {
            throw BaseException(ErrorCode.USER_INFO_REQUIRED, ErrorCode.USER_INFO_REQUIRED.errorMessage)
        }
        val accessToken = jwtUtil.generateAccessToken(user.id, user.information!!.nickname)
        val rotateId = jwtUtil.generateRotateId()
        val refreshToken = jwtUtil.generateRefreshToken(user.id, rotateId)
        jwtUtil.storeCachedRefreshTokenRotateId(user.id, rotateId)
        return ServiceTokensResponse(accessToken, refreshToken)
    }

    @Transactional
    fun kakaoLogin(request: KakaoLoginRequest): KakaoLoginResponse {
        val kakaoUserInfo = getKakaoUserInfo(request.accessToken)
        val providerUserId = requireNotNull(kakaoUserInfo.id) { "해당 계정 정보가 존재하지 않습니다." }
        val user = userRepository.findByProviderTypeAndProviderUserId(ProviderType.KAKAO,providerUserId)
        return user?.let {
            if (request.fcmToken!=null&&request.fcmToken != it.fcmToken) {
                it.changeFcmToken(request.fcmToken)
            }
            val tokens = makeTokens(it)
            KakaoLoginResponse(tokens.accessToken, tokens.refreshToken, isNewUser = false)
        } ?: KakaoLoginResponse(isNewUser = true)
    }

    /*@RedisLock(
        prefix = "userNickname",
        key = "#request.nickname",
        waitTime = 5,
        leaseTime = 3,
    )*/
    @Transactional
    fun createSocialUser(request: UserCreateRequest): ServiceTokensResponse {
        if (userRepository.existsByInformation_Nickname(request.nickname)) {
            throw AlreadyExistNicknameException()
        }
        //TODO:when(request)
        val kakaoUserInfo = getKakaoUserInfo(request.accessToken)
        val providerUserId = requireNotNull(kakaoUserInfo.id) { "해당 계정 정보가 존재하지 않습니다." }
        if (userRepository.existsByProviderTypeAndProviderUserId(request.providerType,providerUserId)) {
            throw AlreadyExistedProviderUserIdException()
        }

        val user =
            userRepository.save(
                User.createSocailUser(
                    providerType = request.providerType,
                    providerUserId = providerUserId,
                    userRole = UserRole.ROLE_USER,

                    /* 갖고 올 수 있는 정보인지 확인
                    email = kakaoUserInfo.email,
                    gender = kakaoUserInfo.gender*/
                ),
            )
        return makeTokens(user)
    }

    @Transactional(readOnly = true)
    fun checkDuplicatedNickName(nickname: String): NicknameCheckResponse =
        NicknameCheckResponse(userRepository.existsByInformation_Nickname(nickname))

    fun getKakaoUserInfo(accessToken: String): KakaoUserInfoResponse = runCatching {
        getKakaoUserInfo(accessToken)
    }.getOrElse { throw KakaoClientException(it.message) }

    @Transactional
    fun signupWithEmail(
        request: EmailSignupRequest,
    ) {
        val existingUser = userRepository.findByEmail(request.email)
        if (existingUser != null) {
            throw BaseException(ErrorCode.ALREADY_EXIST_PROVIDER_USER_ID, ErrorCode.ALREADY_EXIST_PROVIDER_USER_ID.errorMessage)
        }
        val user = createEmailUser(
            email = request.email,
            userRole = UserRole.ROLE_USER,
            password = passwordEncoder.encode(request.password)!!,
        )
        userRepository.save(user)
    }

    @Transactional(readOnly = true)
    fun loginWithEmail(request: EmailLoginRequest): ServiceTokensResponse {
        try {
            val authentication: Authentication =
                authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken(request.email, request.password),
                )
            val userDetails = authentication.principal as CustomUserDto
            val user = userRepository.findById(userDetails.username.toLong()).orElseThrow {
                BaseException(ErrorCode.USER_NOT_FOUND, ErrorCode.USER_NOT_FOUND.errorMessage)}
            return makeTokens(user)
        } catch (e: Exception) {
            throw BaseException(ErrorCode.INVALID_CREDENTIALS, e.message)
        }
    }
    /*@RedisLock(
        prefix = "userNickname",
        key = "#nicknameUpdateRequest.nickname",
        waitTime = 5,
        leaseTime = 3,
    )
    @Transactional
    fun updateNickname(
        nicknameUpdateRequest: NicknameUpdateRequest,
        userId: Long,
    ) {
        if (userRepository.existsByNicknameAndIdNot(nicknameUpdateRequest.nickname, userId)) {
            throw AlreadyExistNicknameException()
        }
        val user = userRepository.findByIdOrNull(userId) ?: throw UserNotFoundException(userId)
        user.updateNickname(nicknameUpdateRequest.nickname)
    }

    @Transactional
    fun updateProfileImage(
        userId: Long,
        profileImage: MultipartFile,
    ) {
        val user = userRepository.findByIdOrNull(userId) ?: throw UserNotFoundException(userId)
        val prevProfileImage = user.profileImage
        val profileImageUrl =
            objectStorageRepository.upload(ObjectStorageRepository.USER_PROFILE_IMAGE_PATH, profileImage)
        prevProfileImage?.let { objectStorageRepository.delete(it) }
        user.profileImage = profileImageUrl
    }

    @Transactional
    fun deleteUser(userId: Long) {
        val user = userRepository.findByIdOrNull(userId) ?: throw UserNotFoundException(userId)
        user.information.profileFileName?.let { objectStorageRepository.delete(it) }
        userRepository.delete(user)
    }

    fun getUsernameByIdToken(idToken: String): String = firebaseTokenHelper.getUid(idToken)



    private fun validateRegisterInformation(registerRequest: RegisterRequest) {
        if (userRepository.existsByUsername(registerRequest.username)) {
            throw ExistResourceException("${registerRequest.username}: 이미 존재하는 회원입니다")
        }

        registerRequest.apply {
            email?.apply { validateEmail(this) }
            phoneNumber?.apply { validatePhoneNumber(this) }
            validateNickname(nickname)
        }
    }

    fun refreshAccessToken(refreshToken: String): TokenResponse {
        val userId = jwtTokenService.extractUserId(refreshToken)
            ?: throw IllegalArgumentException("Invalid refresh token")

        val user = userRepository.findById(userId)
            .orElseThrow { IllegalArgumentException("User not found") }

        val newAccessToken = jwtTokenService.generateAccessToken(user)
        val newRefreshToken = jwtTokenService.generateRefreshToken(user)

        return TokenResponse(
            accessToken = newAccessToken,
            refreshToken = newRefreshToken,
            expiresIn = 3600000L,
            user = TokenResponse.UserInfo(
                id = user.id,
                email = user.email,
                name = user.name,
                profileImageUrl = user.profileImageUrl,
            ),
        )
    }*/
}
