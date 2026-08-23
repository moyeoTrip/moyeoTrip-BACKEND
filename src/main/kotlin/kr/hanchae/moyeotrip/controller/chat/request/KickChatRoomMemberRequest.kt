package kr.hanchae.moyeotrip.controller.chat.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(description = "채팅방 멤버 강퇴 요청")
data class KickChatRoomMemberRequest(
    @field:Schema(
        description = "강퇴된 사용자 본인에게만 제공하고 강퇴 이력에 저장할 사유. 다른 채팅방 멤버에게는 공개되지 않습니다.",
        example = "반복적인 약속 불이행",
    )
    @field:NotBlank(message = "강퇴 사유를 입력해야 합니다.")
    @field:Size(max = 500, message = "강퇴 사유는 500자 이하여야 합니다.")
    val reason: String,
)
