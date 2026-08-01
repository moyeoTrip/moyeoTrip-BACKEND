package kr.hanchae.moyeotrip.controller.user.response

import io.swagger.v3.oas.annotations.media.Schema
import kr.hanchae.moyeotrip.entity.user.SignupState

data class ProfileImageSelectionResponse(
    @field:Schema(description = "현재 프로필로 선택된 이미지")
    val selectedImage: ProfileImageCandidateResponse,
    @field:Schema(description = "이미지 선택 후 회원가입 진행 상태", example = "SIGNUP_COMPLETE")
    val signupState: SignupState,
)
