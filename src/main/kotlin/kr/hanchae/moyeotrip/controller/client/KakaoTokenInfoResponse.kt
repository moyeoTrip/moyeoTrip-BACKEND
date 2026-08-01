package kr.hanchae.moyeotrip.controller.client

import com.fasterxml.jackson.annotation.JsonProperty

data class KakaoTokenInfoResponse(
    val id: Long,
    @field:JsonProperty("app_id")
    val appId: Long,
    @field:JsonProperty("expires_in")
    val expiresInSeconds: Long,
)
