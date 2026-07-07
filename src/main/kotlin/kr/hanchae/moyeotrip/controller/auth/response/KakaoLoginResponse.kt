package kr.hanchae.moyeotrip.controller.auth.response

data class KakaoLoginResponse(
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val isNewUser: Boolean,
)
