package kr.hanchae.moyeotrip.controller.user.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "생성된 프로필 이미지 후보 정보")
data class ProfileImageCandidateResponse(
    @field:Schema(description = "생성된 프로필 이미지 ID", example = "12")
    val profileImageId: Long,
    @field:Schema(
        description = "생성된 이미지의 CDN URL",
        example = "https://cdn.example.com/user/profile/image/550e8400-e29b-41d4-a716-446655440000.png",
    )
    val profileImageUrl: String,
    @field:Schema(description = "현재 프로필 이미지로 선택되었는지 여부", example = "false")
    val selected: Boolean,
)
