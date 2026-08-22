package kr.hanchae.moyeotrip.controller.chat.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size

@Schema(description = "채팅방 참가 신청 요청")
data class JoinChatRoomRequest(
    @field:Schema(description = "호스트에게 전달할 참가 신청 소개", example = "안동 여행이 처음이라 함께 즐기고 싶습니다.", nullable = true)
    @field:Size(max = 500, message = "참가 승인 요청 메시지는 500자 이하여야 합니다.")
    val applicationMessage: String? = null,
)
