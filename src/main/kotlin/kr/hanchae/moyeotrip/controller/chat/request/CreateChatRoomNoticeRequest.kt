package kr.hanchae.moyeotrip.controller.chat.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CreateChatRoomNoticeRequest(
    @field:NotBlank(message = "공지 내용을 입력해야 합니다.")
    @field:Size(max = 1000, message = "공지는 1000자 이하여야 합니다.")
    val notice: String,
)
