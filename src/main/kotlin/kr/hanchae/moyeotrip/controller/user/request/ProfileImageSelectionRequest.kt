package kr.hanchae.moyeotrip.controller.user.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Positive

@Schema(description = "생성한 프로필 이미지 후보 선택 요청")
data class ProfileImageSelectionRequest(
    @field:Schema(
        description = "현재 프로필로 선택할 본인 소유의 생성 이미지 ID",
        example = "12",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    @field:Positive(message = "프로필 이미지 ID는 양수여야 합니다.")
    val profileImageId: Long,
)
