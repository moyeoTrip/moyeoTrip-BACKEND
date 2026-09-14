package kr.hanchae.moyeotrip.controller.chat.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size
import kr.hanchae.moyeotrip.entity.report.ChatRoomReportReason
import kr.hanchae.moyeotrip.entity.report.UserReportReason

// BE-12: 요청 형태는 피드 신고(CreateFeedReportRequest)와 같게 맞췄다 — 사유 코드 + 선택 상세.
@Schema(description = "모집(채팅방) 신고 요청")
data class CreateChatRoomReportRequest(
    @field:Schema(description = "신고 사유", example = "SPAM")
    val reason: ChatRoomReportReason,
    @field:Schema(description = "기타 신고 상세 내용", nullable = true, maxLength = 300)
    @field:Size(max = 300)
    val details: String? = null,
)

@Schema(description = "멤버(사용자) 신고 요청")
data class CreateUserReportRequest(
    @field:Schema(description = "신고 사유", example = "HARASSMENT")
    val reason: UserReportReason,
    @field:Schema(description = "기타 신고 상세 내용", nullable = true, maxLength = 300)
    @field:Size(max = 300)
    val details: String? = null,
)
