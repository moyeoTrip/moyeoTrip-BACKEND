package kr.hanchae.moyeotrip.controller.auth.request

import jakarta.validation.constraints.NotBlank

data class KakaoCustomTokenRequest(
    @field:NotBlank(message = "카카오 액세스 토큰은 필수입니다.")
    val accessToken: String,
)
