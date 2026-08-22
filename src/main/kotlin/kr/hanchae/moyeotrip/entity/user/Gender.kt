package kr.hanchae.moyeotrip.entity.user

import io.swagger.v3.oas.annotations.media.Schema

@Schema(
    description = "성별. M=남성, F=여성, N=선택하지 않음",
    allowableValues = ["M", "F", "N"],
)
enum class Gender(
    val description: String,
) {
    M("남자"),
    F("여자"),
    N("선택 안함"),
}
