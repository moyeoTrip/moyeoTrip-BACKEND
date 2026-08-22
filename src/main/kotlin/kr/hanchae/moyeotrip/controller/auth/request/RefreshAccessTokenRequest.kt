package kr.hanchae.moyeotrip.controller.auth.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "서비스 access·refresh token 재발급 요청")
data class RefreshAccessTokenRequest(
    @field:Schema(
        description = "직전 로그인·가입·재발급 응답에서 받은 모여트립 서비스 refresh token",
        example = "eyJhbGciOiJIUzI1NiJ9.refresh-token-example...",
        requiredMode = Schema.RequiredMode.REQUIRED,
        accessMode = Schema.AccessMode.WRITE_ONLY,
    )
    @field:NotBlank(message = "RefreshToken은 필수입니다.")
    val refreshToken: String,
)
