package kr.hanchae.moyeotrip.controller.chat.request

import io.swagger.v3.oas.annotations.media.Schema

@Schema(
    description = "내 채팅방 목록 상태 필터. ALL=모든 방, RECRUITING=모집 중, CONFIRMED=여행 확정, ENDED=종료된 여행",
    allowableValues = ["ALL", "RECRUITING", "CONFIRMED", "ENDED"],
)
enum class MyChatRoomFilter {
    ALL,
    RECRUITING,
    CONFIRMED,
    ENDED,
}
