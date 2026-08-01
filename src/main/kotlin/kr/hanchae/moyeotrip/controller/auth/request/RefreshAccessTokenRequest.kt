package kr.hanchae.moyeotrip.controller.auth.request

import jakarta.validation.constraints.NotBlank

data class RefreshAccessTokenRequest(
    @field:NotBlank(message = "RefreshToken은 필수입니다.")
    val refreshToken: String,
)
