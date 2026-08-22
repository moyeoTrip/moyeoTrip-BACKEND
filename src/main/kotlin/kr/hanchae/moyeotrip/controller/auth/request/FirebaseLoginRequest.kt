package kr.hanchae.moyeotrip.controller.auth.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "Firebase ID Token으로 서비스 로그인하는 요청")
data class FirebaseLoginRequest(
    @field:Schema(
        description =
            "이메일·Google·Apple 또는 Kakao Firebase 로그인을 완료한 뒤 발급받은 Firebase ID Token. " +
                "Kakao Custom Token 자체가 아니며, 서버가 이 토큰에서 제공자를 자동 판별합니다.",
        example = "eyJhbGciOiJSUzI1NiIsImtpZCI6IjEyMzQ1Njc4OTAifQ...",
        requiredMode = Schema.RequiredMode.REQUIRED,
        accessMode = Schema.AccessMode.WRITE_ONLY,
    )
    @field:NotBlank(message = "Firebase ID 토큰은 필수입니다.")
    val idToken: String,
    @field:Schema(
        description = "푸시 알림에 사용할 최신 Firebase Cloud Messaging 등록 토큰",
        example = "fcm_registration_token_example",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED,
        nullable = true,
    )
    val fcmToken: String? = null,
)
