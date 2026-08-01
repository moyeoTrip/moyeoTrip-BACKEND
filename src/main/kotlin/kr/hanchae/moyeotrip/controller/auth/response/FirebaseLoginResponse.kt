package kr.hanchae.moyeotrip.controller.auth.response

import kr.hanchae.moyeotrip.entity.user.ProviderType

data class FirebaseLoginResponse(
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val isNewUser: Boolean,
    val providerType: ProviderType,
)
