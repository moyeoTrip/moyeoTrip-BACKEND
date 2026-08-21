package kr.hanchae.moyeotrip.controller.chat.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class SendChatMessageRequest(
    @field:NotBlank(message = "메시지를 입력해주세요.")
    @field:Size(max = 1000, message = "메시지는 1000자 이하여야 합니다.")
    val content: String,
    val replyToMessageId: Long? = null,
    val mentionedUserIds: Set<Long> = emptySet(),
)
