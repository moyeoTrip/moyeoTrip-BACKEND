package kr.hanchae.moyeotrip.controller.auth.request

import jakarta.validation.constraints.NotBlank

data class FirebaseLoginRequest(
    @field:NotBlank(message = "Firebase ID 토큰은 필수입니다.")
    val idToken: String,
    val fcmToken: String? = null,
)
