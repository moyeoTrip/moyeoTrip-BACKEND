package kr.hanchae.moyeotrip.controller.auth

import jakarta.validation.Valid
import kr.hanchae.moyeotrip.config.security.CustomUserDto
import kr.hanchae.moyeotrip.controller.auth.request.FirebaseLoginRequest
import kr.hanchae.moyeotrip.controller.auth.request.FirebaseSignupRequest
import kr.hanchae.moyeotrip.controller.auth.request.KakaoAuthorizationCodeRequest
import kr.hanchae.moyeotrip.controller.auth.request.KakaoCustomTokenRequest
import kr.hanchae.moyeotrip.controller.auth.request.RefreshAccessTokenRequest
import kr.hanchae.moyeotrip.controller.auth.response.FirebaseCustomTokenResponse
import kr.hanchae.moyeotrip.controller.auth.response.FirebaseLoginResponse
import kr.hanchae.moyeotrip.controller.auth.response.LinkedProvidersResponse
import kr.hanchae.moyeotrip.controller.auth.response.NicknameCandidatesResponse
import kr.hanchae.moyeotrip.controller.auth.response.ServiceTokensResponse
import kr.hanchae.moyeotrip.service.auth.AuthService
import kr.hanchae.moyeotrip.service.auth.NicknameCandidateService
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
    private val nicknameCandidateService: NicknameCandidateService,
) : AuthAPISpec {
    @PostMapping("/nickname-candidates")
    override fun generateNicknameCandidates(): NicknameCandidatesResponse = nicknameCandidateService.generateCandidates()

    @PostMapping("/firebase/kakao/custom-token")
    override fun createKakaoCustomToken(
        @Valid @RequestBody request: KakaoCustomTokenRequest,
    ): FirebaseCustomTokenResponse = authService.createKakaoCustomToken(request)

    @PostMapping("/firebase/kakao/authorization-code/custom-token")
    override fun createKakaoCustomTokenFromAuthorizationCode(
        @Valid @RequestBody request: KakaoAuthorizationCodeRequest,
    ): FirebaseCustomTokenResponse = authService.createKakaoCustomToken(request)

    @PostMapping("/login")
    override fun login(
        @Valid @RequestBody request: FirebaseLoginRequest,
    ): FirebaseLoginResponse = authService.loginWithFirebase(request)

    @PostMapping("/signup")
    override fun signup(
        @Valid @RequestBody request: FirebaseSignupRequest,
    ): ResponseEntity<ServiceTokensResponse> = ResponseEntity.status(HttpStatus.CREATED).body(authService.signupWithFirebase(request))

    @PostMapping("/providers")
    override fun linkProvider(
        @AuthenticationPrincipal principal: CustomUserDto,
        @Valid @RequestBody request: FirebaseLoginRequest,
    ): LinkedProvidersResponse = authService.linkFirebaseIdentity(principal.username.toLong(), request)

    @GetMapping("/providers")
    override fun getLinkedProviders(
        @AuthenticationPrincipal principal: CustomUserDto,
    ): LinkedProvidersResponse = authService.getLinkedProviders(principal.username.toLong())

    @PostMapping("/refresh")
    override fun refreshAccessToken(
        @Valid @RequestBody request: RefreshAccessTokenRequest,
    ): ServiceTokensResponse = authService.refreshTokens(request)
}
