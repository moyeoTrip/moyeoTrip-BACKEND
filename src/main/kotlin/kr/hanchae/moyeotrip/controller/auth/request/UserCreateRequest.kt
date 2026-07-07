package kr.hanchae.moyeotrip.controller.auth.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import kr.hanchae.moyeotrip.entity.user.ProviderType

data class UserCreateRequest(
    @field:NotBlank(message = "닉네임은 필수입니다.")
    @field:Size(min = 2, max = 15, message = "닉네임은 2자부터 15자이하로 입력 가능합니다.")
    @field:Schema(
        example = "닉네임",
    )
    val nickname: String,
    @field:NotBlank(message = "provider 엑세스 토큰은 필수입니다.")
    @field:Schema(
        example = "provider 액세스 토큰",
    )
    val accessToken: String,
    @field:NotBlank(message = "provider는 필수입니다.")
    @field:Schema(
        description = "provider 타입 (KAKAO, APPLE)",
        allowableValues = ["KAKAO", "APPLE"],
    )
    val providerType: ProviderType
)
