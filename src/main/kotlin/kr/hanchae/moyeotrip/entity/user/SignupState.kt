package kr.hanchae.moyeotrip.entity.user

enum class SignupState(
    val description: String,
) {
    USER_INFO_REQUIRED("회원 정보 입력 필요"),
    PROFILE_IMAGE_REQUIRED("프로필 이미지 선택 필요"),
    SIGNUP_COMPLETE("회원가입 완료"),
}
