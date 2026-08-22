package kr.hanchae.moyeotrip.controller.notification.request

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "채팅방별 알림 수신 설정 변경 요청")
data class UpdateChatRoomNotificationSettingRequest(
    @field:Schema(description = "해당 채팅방 알림 수신 여부", example = "true")
    val enabled: Boolean,
)
