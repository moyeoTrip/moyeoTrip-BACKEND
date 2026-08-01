package kr.hanchae.moyeotrip.controller.user.response

import io.swagger.v3.oas.annotations.media.Schema
import kr.hanchae.moyeotrip.entity.user.SignupState

data class ProfileImageGenerationResponse(
    @field:Schema(description = "이번 요청에서 생성된 후보 이미지")
    val candidate: ProfileImageCandidateResponse,
    @field:Schema(description = "지금까지 성공한 이미지 생성 횟수", example = "1", minimum = "1", maximum = "3")
    val generationCount: Int,
    @field:Schema(description = "앞으로 생성할 수 있는 남은 횟수", example = "2", minimum = "0", maximum = "2")
    val remainingGenerationCount: Int,
    @field:Schema(description = "현재 회원가입 진행 상태", example = "PROFILE_IMAGE_REQUIRED")
    val signupState: SignupState,
)
