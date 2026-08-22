package kr.hanchae.moyeotrip.controller.chat.request

import io.swagger.v3.oas.annotations.media.Schema
import kr.hanchae.moyeotrip.entity.chat.ChatRoomStatus

@Schema(description = "채팅방 여행 상태 변경 요청")
data class UpdateChatRoomStatusRequest(
    @field:Schema(description = "호스트가 변경할 상태. 모집 중인 방은 CONFIRMED 또는 CANCELLED로 변경할 수 있습니다.", example = "CONFIRMED")
    val status: ChatRoomStatus,
)
