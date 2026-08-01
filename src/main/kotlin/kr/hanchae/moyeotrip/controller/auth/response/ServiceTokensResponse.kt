package kr.hanchae.moyeotrip.controller.auth.response

import io.swagger.v3.oas.annotations.media.Schema
import kr.hanchae.moyeotrip.entity.user.SignupState

data class ServiceTokensResponse(
    @field:Schema(
        description = "보호 API의 Authorization Bearer 헤더에 사용할 서비스 access token",
        example = "eyJhbGciOiJIUzI1NiJ9.access...",
        requiredMode = Schema.RequiredMode.REQUIRED,
        accessMode = Schema.AccessMode.READ_ONLY,
    )
    val accessToken: String,
    @field:Schema(
        description = "서비스 토큰 재발급에 사용할 일회전 방식의 refresh token",
        example = "eyJhbGciOiJIUzI1NiJ9.refresh...",
        requiredMode = Schema.RequiredMode.REQUIRED,
        accessMode = Schema.AccessMode.READ_ONLY,
    )
    val refreshToken: String,
    @field:Schema(
        description = "현재 회원가입 진행 상태. PROFILE_IMAGE_REQUIRED이면 프로필 설정 화면으로 이동해야 합니다.",
        example = "PROFILE_IMAGE_REQUIRED",
        allowableValues = ["USER_INFO_REQUIRED", "PROFILE_IMAGE_REQUIRED", "SIGNUP_COMPLETE"],
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val signupState: SignupState,
)
