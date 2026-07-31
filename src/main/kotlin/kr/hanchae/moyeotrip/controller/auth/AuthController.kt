package kr.hanchae.moyeotrip.controller.auth

import jakarta.validation.Valid
import kr.hanchae.moyeotrip.controller.auth.request.EmailLoginRequest
import kr.hanchae.moyeotrip.controller.auth.request.EmailSignupRequest
import kr.hanchae.moyeotrip.controller.auth.request.KakaoLoginRequest
import kr.hanchae.moyeotrip.controller.auth.request.UserCreateRequest
import kr.hanchae.moyeotrip.controller.auth.request.RefreshAccessTokenRequest
import kr.hanchae.moyeotrip.controller.auth.response.KakaoLoginResponse
import kr.hanchae.moyeotrip.controller.auth.response.ServiceTokensResponse
import kr.hanchae.moyeotrip.service.auth.AuthService
import kr.hanchae.moyeotrip.service.auth.UserService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val authService: AuthService,
    private val userService: UserService,
) : AuthControllerSpec {
    @PostMapping("/login/kakao")
    override fun kakaoLogin(
        @RequestBody @Validated kakaoLoginRequest: KakaoLoginRequest,
    ): KakaoLoginResponse = authService.kakaoLogin(kakaoLoginRequest)

    @PostMapping("/user/kakao")
    override fun createKakaoUser(
        @RequestBody @Validated userCreateRequest: UserCreateRequest,
    ) = authService.createSocialUser(userCreateRequest)

    @PostMapping("/signup/email")
    fun signupWithEmail(
        @Valid @RequestBody request: EmailSignupRequest,
    ): ResponseEntity<Unit> {
        authService.signupWithEmail(request)
        return ResponseEntity.status(HttpStatus.CREATED).build()
    }

    @PostMapping("/login/email")
    fun loginWithEmail(
        @Valid @RequestBody request: EmailLoginRequest,
    ): ResponseEntity<ServiceTokensResponse> {
        val tokenResponse: ServiceTokensResponse = authService.loginWithEmail(request)
        return ResponseEntity
            .status(HttpStatus.OK)
            .body((tokenResponse))
    }

    @PostMapping("/refresh")
    override fun refreshAccessToken(
        @RequestBody request: RefreshAccessTokenRequest,
    ): ServiceTokensResponse = authService.refreshTokens(request)
}
