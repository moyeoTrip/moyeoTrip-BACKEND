package kr.hanchae.moyeotrip.controller.chat.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size

data class UpdateChatRoomNoticeRequest(
    @field:Size(max = 1000, message = "공지는 1000자 이하여야 합니다.")
    @Schema(description = "채팅방 공지, null이면 현재 공지를 내림")
    val notice: String?,
)
