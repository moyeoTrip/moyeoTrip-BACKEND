package kr.hanchae.moyeotrip.controller.chat.request

import jakarta.validation.constraints.Size

data class JoinChatRoomRequest(
    @field:Size(max = 500, message = "참가 승인 요청 메시지는 500자 이하여야 합니다.")
    val applicationMessage: String? = null,
)
