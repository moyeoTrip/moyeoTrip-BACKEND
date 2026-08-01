package kr.hanchae.moyeotrip.controller.auth.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class FirebaseSignupRequest(
    @field:NotBlank(message = "Firebase ID 토큰은 필수입니다.")
    val idToken: String,
    @field:NotBlank(message = "닉네임은 필수입니다.")
    @field:Size(min = 2, max = 15, message = "닉네임은 2자부터 15자 이하로 입력 가능합니다.")
    val nickname: String,
    val fcmToken: String? = null,
)
