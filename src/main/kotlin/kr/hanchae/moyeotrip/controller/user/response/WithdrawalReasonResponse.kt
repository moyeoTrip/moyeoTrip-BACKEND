package kr.hanchae.moyeotrip.controller.user.response

import io.swagger.v3.oas.annotations.media.Schema
import kr.hanchae.moyeotrip.entity.user.WithdrawalReason

/**
 * 탈퇴 사유 선택지. 신고 사유(`GET /feeds/report-reasons`)와 **같은 방식**이다 —
 * 문구를 클라이언트가 하드코딩하면 세 플랫폼이 조금씩 어긋난다.
 */
@Schema(description = "회원 탈퇴 사유 선택지")
data class WithdrawalReasonResponse(
    @field:Schema(description = "탈퇴 요청에 전달할 사유 코드", example = "TRAVEL_LESS_OFTEN")
    val reason: WithdrawalReason,
    @field:Schema(description = "화면에 표시할 사유명", example = "여행을 자주 가지 않게 됐어요")
    val displayName: String,
)
