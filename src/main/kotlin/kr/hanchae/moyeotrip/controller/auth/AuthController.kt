package kr.hanchae.moyeotrip.controller.auth

import jakarta.validation.Valid
import kr.hanchae.moyeotrip.config.security.CustomUserDto
import kr.hanchae.moyeotrip.controller.auth.request.FirebaseLoginRequest
import kr.hanchae.moyeotrip.controller.auth.request.FirebaseSignupRequest
import kr.hanchae.moyeotrip.controller.auth.request.KakaoCustomTokenRequest
import kr.hanchae.moyeotrip.controller.auth.request.RefreshAccessTokenRequest
import kr.hanchae.moyeotrip.controller.auth.response.FirebaseCustomTokenResponse
import kr.hanchae.moyeotrip.controller.auth.response.FirebaseLoginResponse
import kr.hanchae.moyeotrip.controller.auth.response.LinkedProvidersResponse
import kr.hanchae.moyeotrip.controller.auth.response.ServiceTokensResponse
import kr.hanchae.moyeotrip.entity.user.ProviderType
import kr.hanchae.moyeotrip.service.auth.AuthService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val authService: AuthService,
) : AuthAPISpec {
    @PostMapping("/firebase/kakao/custom-token")
    override fun createKakaoCustomToken(
        @Valid @RequestBody request: KakaoCustomTokenRequest,
    ): FirebaseCustomTokenResponse = authService.createKakaoCustomToken(request)

    @PostMapping("/login/firebase")
    override fun loginWithFirebase(
        @Valid @RequestBody request: FirebaseLoginRequest,
    ): FirebaseLoginResponse = authService.loginWithFirebase(request)

    @PostMapping("/login/kakao")
    override fun loginWithKakao(
        @Valid @RequestBody request: FirebaseLoginRequest,
    ): FirebaseLoginResponse = authService.loginWithFirebase(request, ProviderType.KAKAO)

    @PostMapping("/login/email")
    override fun loginWithEmail(
        @Valid @RequestBody request: FirebaseLoginRequest,
    ): FirebaseLoginResponse = authService.loginWithFirebase(request, ProviderType.EMAIL)

    @PostMapping("/login/apple")
    override fun loginWithApple(
        @Valid @RequestBody request: FirebaseLoginRequest,
    ): FirebaseLoginResponse = authService.loginWithFirebase(request, ProviderType.APPLE)

    @PostMapping("/signup/firebase")
    override fun signupWithFirebase(
        @Valid @RequestBody request: FirebaseSignupRequest,
    ): ResponseEntity<ServiceTokensResponse> = ResponseEntity.status(HttpStatus.CREATED).body(authService.signupWithFirebase(request))

    @PostMapping("/user/kakao")
    override fun signupWithKakao(
        @Valid @RequestBody request: FirebaseSignupRequest,
    ): ResponseEntity<ServiceTokensResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(authService.signupWithFirebase(request, ProviderType.KAKAO))

    @PostMapping("/signup/email")
    override fun signupWithEmail(
        @Valid @RequestBody request: FirebaseSignupRequest,
    ): ResponseEntity<ServiceTokensResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(authService.signupWithFirebase(request, ProviderType.EMAIL))

    @PostMapping("/signup/apple")
    override fun signupWithApple(
        @Valid @RequestBody request: FirebaseSignupRequest,
    ): ResponseEntity<ServiceTokensResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(authService.signupWithFirebase(request, ProviderType.APPLE))

    @PostMapping("/providers/firebase")
    override fun linkFirebaseProvider(
        @AuthenticationPrincipal principal: CustomUserDto,
        @Valid @RequestBody request: FirebaseLoginRequest,
    ): LinkedProvidersResponse = authService.linkFirebaseIdentity(principal.username.toLong(), request)

    @PostMapping("/providers/kakao")
    override fun linkKakaoProvider(
        @AuthenticationPrincipal principal: CustomUserDto,
        @Valid @RequestBody request: KakaoCustomTokenRequest,
    ): LinkedProvidersResponse = authService.linkKakaoIdentity(principal.username.toLong(), request)

    @GetMapping("/providers")
    override fun getLinkedProviders(
        @AuthenticationPrincipal principal: CustomUserDto,
    ): LinkedProvidersResponse = authService.getLinkedProviders(principal.username.toLong())

    @PostMapping("/refresh")
    override fun refreshAccessToken(
        @Valid @RequestBody request: RefreshAccessTokenRequest,
    ): ServiceTokensResponse = authService.refreshTokens(request)
}
