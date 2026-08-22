package kr.hanchae.moyeotrip.controller.terms.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "회원가입 약관 목록 항목")
data class AgreementTermSummaryResponse(
    @field:Schema(description = "약관 ID. 회원가입 요청의 agreedTermIds에 사용합니다.", example = "1")
    val termId: Long,
    @field:Schema(description = "약관 제목", example = "[필수] 모여트립 이용약관")
    val title: String,
    @field:Schema(description = "필수 동의 약관 여부. false이면 선택 동의 약관입니다.", example = "true")
    val required: Boolean,
)

@Schema(description = "회원가입 약관 상세")
data class AgreementTermDetailResponse(
    @field:Schema(description = "약관 ID", example = "1")
    val termId: Long,
    @field:Schema(description = "약관 제목", example = "[필수] 모여트립 이용약관")
    val title: String,
    @field:Schema(description = "필수 동의 약관 여부. false이면 선택 동의 약관입니다.", example = "true")
    val required: Boolean,
    @field:Schema(description = "약관 버전", example = "2026.08.23")
    val version: String,
    @field:Schema(description = "Markdown 형식의 약관 본문", example = "# 모여트립 이용약관\\n\\n## 제1조 목적")
    val content: String,
)
