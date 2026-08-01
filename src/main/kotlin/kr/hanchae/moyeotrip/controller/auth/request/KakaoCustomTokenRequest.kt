package kr.hanchae.moyeotrip.controller.auth.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

data class KakaoCustomTokenRequest(
    @field:Schema(
        description = "카카오 SDK 로그인으로 발급된 access token. Firebase 토큰이나 카카오 인가 코드가 아닙니다.",
        example = "kakao_access_token_example",
        requiredMode = Schema.RequiredMode.REQUIRED,
        accessMode = Schema.AccessMode.WRITE_ONLY,
    )
    @field:NotBlank(message = "카카오 액세스 토큰은 필수입니다.")
    val accessToken: String,
)
