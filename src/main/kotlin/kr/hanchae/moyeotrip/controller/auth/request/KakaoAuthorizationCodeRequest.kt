package kr.hanchae.moyeotrip.controller.auth.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(description = "카카오 인가 코드로 Firebase Custom Token을 발급하는 요청")
data class KakaoAuthorizationCodeRequest(
    @field:Schema(
        description = "Kakao.Auth.authorize()가 redirect URI로 전달한 일회성 인가 코드",
        example = "authorization_code_from_kakao",
        requiredMode = Schema.RequiredMode.REQUIRED,
        accessMode = Schema.AccessMode.WRITE_ONLY,
    )
    @field:NotBlank(message = "카카오 인가 코드는 필수입니다.")
    @field:Size(max = 2048, message = "카카오 인가 코드가 너무 깁니다.")
    val code: String,
    @field:Schema(
        description = "인가 코드 요청에 사용한 redirect URI. 서버 허용 목록 및 Kakao Developers 등록값과 정확히 일치해야 합니다.",
        example = "https://moyeotrip.github.io/moyeoTrip-Web/auth/kakao/callback",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    @field:NotBlank(message = "카카오 redirect URI는 필수입니다.")
    @field:Size(max = 2048, message = "카카오 redirect URI가 너무 깁니다.")
    val redirectUri: String,
)
