package kr.hanchae.moyeotrip.api.dto.auth

data class TokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val user: UserInfo,
) {
    data class UserInfo(
        val id: Long,
        val nickname: String,
        val profileImageUrl: String?,
    )
}
