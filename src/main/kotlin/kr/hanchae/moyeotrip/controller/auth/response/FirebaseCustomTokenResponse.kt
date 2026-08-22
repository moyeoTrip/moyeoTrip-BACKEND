package kr.hanchae.moyeotrip.controller.auth.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "카카오 로그인에 사용할 Firebase Custom Token 응답")
data class FirebaseCustomTokenResponse(
    @field:Schema(
        description = "Firebase SDK signInWithCustomToken에 전달할 일회성 Custom Token",
        example = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
        requiredMode = Schema.RequiredMode.REQUIRED,
        accessMode = Schema.AccessMode.READ_ONLY,
    )
    val customToken: String,
)
