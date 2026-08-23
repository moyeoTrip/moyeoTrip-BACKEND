package kr.hanchae.moyeotrip.controller.notification.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(description = "FCM 등록 토큰 등록·갱신 요청")
data class UpdateFcmTokenRequest(
    @field:Schema(
        description = "Android 또는 iOS Firebase SDK에서 발급받은 최신 FCM 등록 토큰",
        example = "fcm_registration_token_example",
        requiredMode = Schema.RequiredMode.REQUIRED,
        accessMode = Schema.AccessMode.WRITE_ONLY,
    )
    @field:NotBlank(message = "FCM 토큰은 필수입니다.")
    @field:Size(max = 255, message = "FCM 토큰은 255자 이하여야 합니다.")
    val fcmToken: String,
)
