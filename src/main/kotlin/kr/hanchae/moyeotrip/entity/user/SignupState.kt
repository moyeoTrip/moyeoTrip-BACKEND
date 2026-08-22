package kr.hanchae.moyeotrip.entity.user

import io.swagger.v3.oas.annotations.media.Schema

@Schema(
    description = "회원가입 진행 상태. USER_INFO_REQUIRED=닉네임·성별·생년월일 입력 필요, PROFILE_IMAGE_REQUIRED=프로필 이미지 선택 필요, SIGNUP_COMPLETE=가입 완료",
    allowableValues = ["USER_INFO_REQUIRED", "PROFILE_IMAGE_REQUIRED", "SIGNUP_COMPLETE"],
)
enum class SignupState(
    val description: String,
) {
    USER_INFO_REQUIRED("회원 정보 입력 필요"),
    PROFILE_IMAGE_REQUIRED("프로필 이미지 선택 필요"),
    SIGNUP_COMPLETE("회원가입 완료"),
}
