package kr.hanchae.moyeotrip.controller.auth.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

data class KakaoLoginRequest(
    @field:Schema(description = "카카오 로그인시 발급받은 엑세스 토큰")
    @field:NotBlank(message = "provider 엑세스 토큰은 필수입니다.")
    val accessToken: String,
    // FCM 토큰은 여러가지 이유로 만료될 수 있으나 로그인할때 받아두면 충분히 최신 토큰을 DB에 저장해둘 수 있다.
    @field:Schema(description = "Firebase FCM 토큰 (선택사항)", example = "fcm_token_example")
    val fcmToken: String?,
)
