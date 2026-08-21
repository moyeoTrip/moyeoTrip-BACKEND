package kr.hanchae.moyeotrip.controller.chat.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class ShareTourismContentRequest(
    val contentId: Long,
)

data class CreateChatPollRequest(
    @field:NotBlank @field:Size(max = 200)
    val question: String,
    @field:Size(min = 2, max = 5)
    val options: List<
        @NotBlank
        @Size(max = 100)
        String,
    >,
    val anonymous: Boolean = true,
)

data class CreateSettlementMemoRequest(
    @field:NotBlank @field:Size(max = 1000)
    val memo: String,
)
