package kr.hanchae.moyeotrip.controller.chat.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(description = "채팅방 공지 등록 요청")
data class CreateChatRoomNoticeRequest(
    @field:Schema(description = "공지 내용", example = "집합 시간 10분 전까지 안동역 1번 출구로 와주세요.")
    @field:NotBlank(message = "공지 내용을 입력해야 합니다.")
    @field:Size(max = 1000, message = "공지는 1000자 이하여야 합니다.")
    val notice: String,
    @field:Schema(description = "상단 고정 공지 여부", example = "true")
    val pinned: Boolean = false,
)
