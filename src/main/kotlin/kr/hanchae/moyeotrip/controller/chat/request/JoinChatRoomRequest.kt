package kr.hanchae.moyeotrip.controller.chat.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class JoinChatRoomRequest(
    @field:NotBlank(message = "참가 승인을 위해 호스트에게 전할 말을 입력해주세요.")
    @field:Size(max = 500, message = "참가 승인 요청 메시지는 500자 이하여야 합니다.")
    val applicationMessage: String,
)
