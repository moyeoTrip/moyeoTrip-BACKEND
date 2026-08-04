package kr.hanchae.moyeotrip.controller.client

import com.fasterxml.jackson.annotation.JsonProperty

data class KakaoOAuthTokenResponse(
    @field:JsonProperty("access_token")
    val accessToken: String,
)
