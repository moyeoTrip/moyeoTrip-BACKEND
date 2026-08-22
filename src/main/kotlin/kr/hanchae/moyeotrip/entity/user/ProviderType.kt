package kr.hanchae.moyeotrip.entity.user

import io.swagger.v3.oas.annotations.media.Schema

@Schema(
    description = "Firebase에서 판별한 로그인 제공자. EMAIL=이메일·비밀번호, KAKAO=카카오, APPLE=Apple, GOOGLE=Google",
    allowableValues = ["EMAIL", "KAKAO", "APPLE", "GOOGLE"],
)
enum class ProviderType {
    EMAIL,
    KAKAO,
    APPLE,
    GOOGLE,
}
