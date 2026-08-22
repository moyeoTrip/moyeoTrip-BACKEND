package kr.hanchae.moyeotrip.controller.auth.response

import io.swagger.v3.oas.annotations.media.Schema
import kr.hanchae.moyeotrip.entity.user.ProviderType
import kr.hanchae.moyeotrip.entity.user.SignupState

@Schema(description = "Firebase 로그인 결과와 회원가입 진행 상태")
data class FirebaseLoginResponse(
    @field:Schema(
        description = "기존 회원에게 발급되는 서비스 access token. 신규 회원이면 null",
        example = "eyJhbGciOiJIUzI1NiJ9.access...",
        nullable = true,
        accessMode = Schema.AccessMode.READ_ONLY,
    )
    val accessToken: String? = null,
    @field:Schema(
        description = "기존 회원에게 발급되는 서비스 refresh token. 신규 회원이면 null",
        example = "eyJhbGciOiJIUzI1NiJ9.refresh...",
        nullable = true,
        accessMode = Schema.AccessMode.READ_ONLY,
    )
    val refreshToken: String? = null,
    @field:Schema(
        description = "추가 회원가입 정보 입력 필요 여부",
        example = "false",
        requiredMode = Schema.RequiredMode.REQUIRED,
        accessMode = Schema.AccessMode.READ_ONLY,
    )
    val isNewUser: Boolean,
    @field:Schema(
        description =
            "서버가 관리하는 회원가입 진행 상태. 클라이언트 로컬 상태보다 이 값을 우선하며, " +
                "다른 기기에서도 PROFILE_IMAGE_REQUIRED이면 발급된 서비스 토큰으로 " +
                "프로필 이미지 생성·조회·선택을 이어서 진행해야 합니다.",
        example = "SIGNUP_COMPLETE",
        allowableValues = ["USER_INFO_REQUIRED", "PROFILE_IMAGE_REQUIRED", "SIGNUP_COMPLETE"],
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val signupState: SignupState,
    @field:Schema(
        description = "Firebase 토큰에서 판별한 로그인 제공자",
        example = "EMAIL",
        allowableValues = ["EMAIL", "KAKAO", "APPLE", "GOOGLE"],
        requiredMode = Schema.RequiredMode.REQUIRED,
        accessMode = Schema.AccessMode.READ_ONLY,
    )
    val providerType: ProviderType,
)
