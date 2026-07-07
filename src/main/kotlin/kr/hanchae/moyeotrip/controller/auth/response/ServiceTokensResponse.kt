package kr.hanchae.moyeotrip.controller.auth.response

data class ServiceTokensResponse(
    val accessToken: String,
    val refreshToken: String,
)
