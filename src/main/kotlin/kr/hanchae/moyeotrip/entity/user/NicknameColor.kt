package kr.hanchae.moyeotrip.entity.user

import io.swagger.v3.oas.annotations.media.Schema

@Schema(
    description =
        "자동 생성 닉네임에 사용하는 표시 색상. RED=빨강, ORANGE=주황, YELLOW=노랑, GREEN=초록, BLUE=파랑, " +
            "NAVY=남색, PURPLE=보라, PINK=분홍, SKY_BLUE=하늘색, MINT=민트",
    allowableValues = ["RED", "ORANGE", "YELLOW", "GREEN", "BLUE", "NAVY", "PURPLE", "PINK", "SKY_BLUE", "MINT"],
)
enum class NicknameColor(
    val imageColorName: String,
    val imagePrimaryHex: String,
    val imageShadowHex: String,
    val imageHighlightHex: String,
) {
    RED("clear true red", "#D62F2F", "#B51F28", "#EF5550"),
    ORANGE("warm orange", "#E97832", "#C95724", "#F39A5A"),
    YELLOW("warm golden yellow", "#F2C84B", "#D5A72E", "#F8DB79"),
    GREEN("natural leaf green", "#4E9B61", "#347647", "#76B985"),
    BLUE("clear medium blue", "#367FB5", "#245F8A", "#67A7D0"),
    NAVY("deep navy blue", "#294C72", "#19354F", "#557697"),
    PURPLE("soft clear purple", "#7B5AA6", "#5D3E83", "#A084C1"),
    PINK("warm rose pink", "#D96E8A", "#B94D6B", "#E89AAF"),
    SKY_BLUE("clear sky blue", "#59A9D8", "#3886B5", "#89C6E7"),
    MINT("fresh mint green", "#58B99A", "#388F75", "#8BD1B9"),
}
