package kr.hanchae.moyeotrip.controller.auth.response

data class TestAccessTokenResponse(
    val userId: Long,
    val nickname: String,
    val accessToken: String,
)
