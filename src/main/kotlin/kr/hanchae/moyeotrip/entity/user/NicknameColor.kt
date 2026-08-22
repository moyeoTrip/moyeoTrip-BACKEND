package kr.hanchae.moyeotrip.entity.user

import io.swagger.v3.oas.annotations.media.Schema

@Schema(
    description =
        "자동 생성 닉네임에 사용하는 표시 색상. RED=빨강, ORANGE=주황, YELLOW=노랑, GREEN=초록, BLUE=파랑, " +
            "NAVY=남색, PURPLE=보라, PINK=분홍, SKY_BLUE=하늘색, MINT=민트",
    allowableValues = ["RED", "ORANGE", "YELLOW", "GREEN", "BLUE", "NAVY", "PURPLE", "PINK", "SKY_BLUE", "MINT"],
)
enum class NicknameColor {
    RED,
    ORANGE,
    YELLOW,
    GREEN,
    BLUE,
    NAVY,
    PURPLE,
    PINK,
    SKY_BLUE,
    MINT,
}
