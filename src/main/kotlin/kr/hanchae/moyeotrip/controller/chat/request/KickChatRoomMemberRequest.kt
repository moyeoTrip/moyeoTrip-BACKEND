package kr.hanchae.moyeotrip.controller.chat.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class KickChatRoomMemberRequest(
    @field:NotBlank(message = "강퇴 사유를 입력해야 합니다.")
    @field:Size(max = 500, message = "강퇴 사유는 500자 이하여야 합니다.")
    val reason: String,
)
