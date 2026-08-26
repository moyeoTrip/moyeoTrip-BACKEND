package kr.hanchae.moyeotrip.controller.chat.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size

@Schema(description = "채팅방 공지 수정 또는 삭제 요청")
data class UpdateChatRoomNoticeRequest(
    @field:Size(max = 1000, message = "공지는 1000자 이하여야 합니다.")
    @field:Schema(description = "수정할 공지 내용", nullable = true)
    val notice: String? = null,
    @field:Schema(description = "변경할 상단 고정 여부. 내용만 수정할 때는 null", nullable = true)
    val pinned: Boolean? = null,
)
