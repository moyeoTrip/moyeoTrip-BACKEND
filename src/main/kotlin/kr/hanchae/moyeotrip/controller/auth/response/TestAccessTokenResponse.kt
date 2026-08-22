package kr.hanchae.moyeotrip.controller.auth.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "로컬·개발 환경 테스트용 서비스 access token 응답")
data class TestAccessTokenResponse(
    @field:Schema(description = "토큰을 발급한 사용자 ID", example = "1")
    val userId: Long,
    @field:Schema(description = "토큰에 포함된 사용자 닉네임", example = "따스한 사슴 3492")
    val nickname: String,
    @field:Schema(description = "Authorization Bearer 헤더에 사용할 테스트 access token", accessMode = Schema.AccessMode.READ_ONLY)
    val accessToken: String,
)
