package kr.hanchae.moyeotrip.controller.auth.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Past
import jakarta.validation.constraints.Size
import kr.hanchae.moyeotrip.entity.user.Gender
import java.time.LocalDate

@Schema(description = "Firebase 인증 사용자 회원가입 요청")
data class FirebaseSignupRequest(
    @field:Schema(
        description =
            "회원가입에 사용할 Firebase ID Token. 이메일·Google·Apple·Kakao를 서버가 토큰에서 자동 판별하며 " +
                "Kakao Custom Token 자체는 허용하지 않습니다.",
        example = "eyJhbGciOiJSUzI1NiIsImtpZCI6IjEyMzQ1Njc4OTAifQ...",
        requiredMode = Schema.RequiredMode.REQUIRED,
        accessMode = Schema.AccessMode.WRITE_ONLY,
    )
    @field:NotBlank(message = "Firebase ID 토큰은 필수입니다.")
    val idToken: String,
    @field:Schema(
        description = "닉네임 후보 API가 발급한 일회성 선택 토큰",
        example = "550e8400-e29b-41d4-a716-446655440000",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    @field:NotBlank(message = "닉네임 선택 토큰은 필수입니다.")
    val nicknameSelectionToken: String,
    @field:Schema(
        description = "서버가 제시한 3개 후보 중 사용자가 선택한 닉네임",
        example = "따스한 사슴 1234",
        minLength = 2,
        maxLength = 24,
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    @field:NotBlank(message = "닉네임은 필수입니다.")
    @field:Size(min = 2, max = 24, message = "닉네임은 2자부터 24자 이하로 입력 가능합니다.")
    val nickname: String,
    @field:Schema(
        description = "회원 프로필에 저장할 성별",
        example = "F",
        allowableValues = ["M", "F", "N"],
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val gender: Gender,
    @field:Schema(
        description = "회원 생년월일. 미래 날짜는 허용하지 않습니다.",
        example = "1998-04-12",
        type = "string",
        format = "date",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    @field:Past(message = "생년월일은 과거 날짜여야 합니다.")
    val birthDate: LocalDate,
    @field:Schema(
        description = "푸시 알림에 사용할 최신 Firebase Cloud Messaging 등록 토큰",
        example = "fcm_registration_token_example",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED,
        nullable = true,
    )
    val fcmToken: String? = null,
    @field:Schema(
        description = "회원가입 시 동의한 현재 활성 약관 ID 목록. 필수 약관 ID는 모두 포함해야 하며, 마케팅 약관 ID를 제외하면 마케팅 알림은 기본 비활성화됩니다.",
        example = "[1, 2, 3]",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val agreedTermIds: Set<Long> = emptySet(),
)
