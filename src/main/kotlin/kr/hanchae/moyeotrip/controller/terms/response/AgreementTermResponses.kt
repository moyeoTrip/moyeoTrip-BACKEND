package kr.hanchae.moyeotrip.controller.terms.response

import io.swagger.v3.oas.annotations.media.Schema
import kr.hanchae.moyeotrip.entity.terms.AgreementTermCode

@Schema(description = "회원가입 약관 목록 항목")
data class AgreementTermSummaryResponse(
    @field:Schema(description = "약관 ID. 회원가입 요청의 agreedTermIds에 사용합니다.", example = "1")
    val termId: Long,
    @field:Schema(
        description =
            "약관 종류를 가리키는 안정적인 식별자. 특정 약관을 열 때는 제목이 아니라 이 값으로 찾습니다. " +
                "termId는 판본마다 새로 발급되므로 종류를 가리키는 용도로 쓸 수 없습니다.",
        example = "PRIVACY_POLICY",
    )
    val code: AgreementTermCode,
    @field:Schema(description = "약관 제목", example = "[필수] 모여트립 이용약관")
    val title: String,
    @field:Schema(description = "필수 동의 약관 여부. false이면 선택 동의 약관입니다.", example = "true")
    val required: Boolean,
    @field:Schema(description = "약관 버전", example = "2026.08.23")
    val version: String,
)

@Schema(description = "회원가입 약관 상세")
data class AgreementTermDetailResponse(
    @field:Schema(description = "약관 ID", example = "1")
    val termId: Long,
    @field:Schema(description = "약관 종류를 가리키는 안정적인 식별자", example = "PRIVACY_POLICY")
    val code: AgreementTermCode,
    @field:Schema(description = "약관 제목", example = "[필수] 모여트립 이용약관")
    val title: String,
    @field:Schema(description = "필수 동의 약관 여부. false이면 선택 동의 약관입니다.", example = "true")
    val required: Boolean,
    @field:Schema(description = "약관 버전", example = "2026.08.23")
    val version: String,
    @field:Schema(description = "Markdown 형식의 약관 본문", example = "# 모여트립 이용약관\\n\\n## 제1조 목적")
    val content: String,
)
