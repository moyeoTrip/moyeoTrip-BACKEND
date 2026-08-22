package kr.hanchae.moyeotrip.controller.user.response

import io.swagger.v3.oas.annotations.media.Schema
import kr.hanchae.moyeotrip.entity.user.SignupState

@Schema(description = "생성한 프로필 이미지 후보 목록과 생성 가능 횟수")
data class ProfileImageCandidatesResponse(
    @field:Schema(description = "사용자가 생성한 이미지 후보. 생성 순서대로 반환됩니다.")
    val candidates: List<ProfileImageCandidateResponse>,
    @field:Schema(description = "지금까지 성공한 이미지 생성 횟수", example = "3", minimum = "0", maximum = "3")
    val generationCount: Int,
    @field:Schema(description = "앞으로 생성할 수 있는 남은 횟수", example = "0", minimum = "0", maximum = "3")
    val remainingGenerationCount: Int,
    @field:Schema(description = "현재 회원가입 진행 상태", example = "PROFILE_IMAGE_REQUIRED")
    val signupState: SignupState,
)
