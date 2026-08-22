package kr.hanchae.moyeotrip.controller.chat.request

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "내 채팅방 목록 상태 필터", allowableValues = ["ALL", "RECRUITING", "CONFIRMED", "ENDED"])
enum class MyChatRoomFilter {
    ALL,
    RECRUITING,
    CONFIRMED,
    ENDED,
}
