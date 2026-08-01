package kr.hanchae.moyeotrip.controller.auth.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

data class FirebaseLoginRequest(
    @field:Schema(
        description = "Firebase Authentication SDK가 발급한 ID Token. Firebase Custom Token 자체가 아닙니다.",
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
