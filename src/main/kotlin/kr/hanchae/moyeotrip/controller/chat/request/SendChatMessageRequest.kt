package kr.hanchae.moyeotrip.controller.chat.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(description = "일반 채팅 메시지 전송 요청")
data class SendChatMessageRequest(
    @field:Schema(description = "전송할 메시지 내용", example = "주왕산 3폭포에 도착했어요!")
    @field:NotBlank(message = "메시지를 입력해주세요.")
    @field:Size(max = 1000, message = "메시지는 1000자 이하여야 합니다.")
    val content: String,
    @field:Schema(description = "답글을 남길 원본 메시지 ID", example = "1024", nullable = true)
    val replyToMessageId: Long? = null,
    @field:Schema(description = "멘션할 현재 채팅방 참가자 ID 목록", example = "[12, 34]")
    val mentionedUserIds: Set<Long> = emptySet(),
)
