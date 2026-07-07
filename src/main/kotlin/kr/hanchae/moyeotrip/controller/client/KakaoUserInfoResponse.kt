package kr.hanchae.moyeotrip.controller.client

import com.fasterxml.jackson.annotation.JsonProperty
import kr.hanchae.moyeotrip.entity.user.Gender

data class KakaoUserInfoResponse(
    val id: String,
    @JsonProperty("kakao_account")
    val kakaoAccount: KakaoAccount = KakaoAccount(),
) {
    data class KakaoAccount(
        val email: String? = null,
        val gender: String? = null,
    )

    val email: String? = kakaoAccount.email
    val gender: Gender = genderFormat()

    private fun genderFormat(): Gender =
        if (!kakaoAccount.gender.isNullOrBlank()) {
            Gender.valueOf(kakaoAccount.gender[0].toString().uppercase())
        } else {
            Gender.N
        }
}
