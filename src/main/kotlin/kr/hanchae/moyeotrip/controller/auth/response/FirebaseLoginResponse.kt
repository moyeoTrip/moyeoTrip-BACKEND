package kr.hanchae.moyeotrip.controller.auth.response

import io.swagger.v3.oas.annotations.media.Schema
import kr.hanchae.moyeotrip.entity.user.ProviderType

data class FirebaseLoginResponse(
    @field:Schema(
        description = "기존 회원에게 발급되는 서비스 access token. 신규 회원이면 null",
        example = "eyJhbGciOiJIUzI1NiJ9.access...",
        nullable = true,
        accessMode = Schema.AccessMode.READ_ONLY,
    )
    val accessToken: String? = null,
    @field:Schema(
        description = "기존 회원에게 발급되는 서비스 refresh token. 신규 회원이면 null",
        example = "eyJhbGciOiJIUzI1NiJ9.refresh...",
        nullable = true,
        accessMode = Schema.AccessMode.READ_ONLY,
    )
    val refreshToken: String? = null,
    @field:Schema(
        description = "추가 회원가입 정보 입력 필요 여부",
        example = "false",
        requiredMode = Schema.RequiredMode.REQUIRED,
        accessMode = Schema.AccessMode.READ_ONLY,
    )
    val isNewUser: Boolean,
    @field:Schema(
        description = "Firebase 토큰에서 판별한 로그인 제공자",
        example = "EMAIL",
        allowableValues = ["EMAIL", "KAKAO", "APPLE"],
        requiredMode = Schema.RequiredMode.REQUIRED,
        accessMode = Schema.AccessMode.READ_ONLY,
    )
    val providerType: ProviderType,
)
