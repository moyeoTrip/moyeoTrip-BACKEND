package kr.hanchae.moyeotrip.controller.chat.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

@Schema(description = "채팅방 공지 수정 또는 삭제 요청")
data class UpdateChatRoomNoticeRequest(
    @field:Size(max = 1000, message = "공지는 1000자 이하여야 합니다.")
    @field:Schema(description = "수정할 공지 내용", nullable = true)
    val notice: String? = null,
    @field:Schema(description = "변경할 상단 고정 여부. 내용만 수정할 때는 null", nullable = true)
    val pinned: Boolean? = null,
    // BE-30 · 화면이 **자기가 읽은** 공지의 `updatedAt` 을 그대로 실어 보낸다.
    // 그 사이 다른 사람이 고쳤으면 서버가 409 로 거절해, 앞사람 수정을 덮어쓰지 않는다.
    // null 이면 검사하지 않는다 — 옛 클라이언트가 바로 깨지지 않게 하려는 것이고,
    // 세 플랫폼이 모두 보내기 시작하면 필수로 올린다.
    @field:Schema(
        description = "수정하려는 공지를 읽었을 때의 최종 수정 일시. 그 사이 다른 사람이 고쳤으면 409(40923). 생략하면 검사하지 않는다",
        nullable = true,
        example = "2026-09-01T12:30:00",
    )
    val expectedUpdatedAt: LocalDateTime? = null,
)
