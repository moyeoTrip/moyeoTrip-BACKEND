package kr.hanchae.moyeotrip.controller.chat.request

import kr.hanchae.moyeotrip.entity.chat.ChatRoomStatus

data class UpdateChatRoomStatusRequest(
    val status: ChatRoomStatus,
)
