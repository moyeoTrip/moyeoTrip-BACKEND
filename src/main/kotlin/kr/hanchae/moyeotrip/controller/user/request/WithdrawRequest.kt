package kr.hanchae.moyeotrip.controller.user.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size
import kr.hanchae.moyeotrip.entity.user.WithdrawalReason

@Schema(description = "회원 탈퇴 요청. 사유는 선택이며, 본문 없이 호출해도 탈퇴됩니다.")
data class WithdrawRequest(
    @field:Schema(
        description = "탈퇴 화면에서 고른 사유. 누가 골랐는지는 저장하지 않고 사유와 시각만 남깁니다.",
        example = "TRAVEL_LESS_OFTEN",
        nullable = true,
    )
    val reason: WithdrawalReason? = null,
    @field:Schema(
        description = "reason이 OTHER일 때의 자유 입력. 다른 사유와 함께 보내면 무시합니다.",
        example = "앱이 무거워요",
        nullable = true,
    )
    @field:Size(max = 200, message = "탈퇴 사유는 200자 이하여야 합니다.")
    val reasonDetail: String? = null,
)
