package kr.hanchae.moyeotrip.controller.chat.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size

@Schema(description = "채팅방 참가 신청 요청")
data class JoinChatRoomRequest(
    @field:Schema(
        description = "호스트에게 전달할 참가 신청 한마디. 입력하면 공백을 제외하고 10자 이상 200자 이하여야 합니다. 수동 승인 모임에서는 필수입니다.",
        example = "안동 여행이 처음이라 함께 즐기고 싶습니다.",
        nullable = true,
        minLength = 10,
        maxLength = 200,
    )
    // BE-08: 세 플랫폼 화면이 모두 「10자 이상 200자 이하」라고 안내하는데 서버는 max=500 뿐이었다.
    // null 은 그대로 허용한다(한마디 없이 신청하는 자동 승인 경로). 공백만 있는 문자열은 서비스에서 따로 막는다.
    @field:Size(min = 10, max = 200, message = "참가 신청 한마디는 10자 이상 200자 이하여야 합니다.")
    val applicationMessage: String? = null,
)
