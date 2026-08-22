package kr.hanchae.moyeotrip.controller.notification

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import kr.hanchae.moyeotrip.controller.notification.request.UpdateChatRoomNotificationSettingRequest
import kr.hanchae.moyeotrip.controller.notification.request.UpdateNotificationSettingRequest
import kr.hanchae.moyeotrip.controller.notification.response.ChatRoomNotificationSettingResponse
import kr.hanchae.moyeotrip.controller.notification.response.NotificationPageResponse
import kr.hanchae.moyeotrip.controller.notification.response.NotificationSettingResponse
import org.springframework.http.ResponseEntity

@Tag(name = "알림", description = "모임 및 채팅 알림 API")
@SecurityRequirement(name = "Authorization")
interface NotificationAPISpec {
    @Operation(summary = "내 알림 목록", description = "lastId 이전의 알림을 최신순으로 조회합니다. unreadOnly가 true면 읽지 않은 알림만 반환합니다.")
    fun getNotifications(
        @Parameter(hidden = true) userId: Long,
        lastId: Long?,
        size: Int,
        unreadOnly: Boolean,
    ): NotificationPageResponse

    @Operation(summary = "알림 읽음 처리", description = "로그인 사용자 소유의 알림 한 건을 읽음 상태로 변경합니다.")
    fun markRead(
        @Parameter(hidden = true) userId: Long,
        notificationId: Long,
    ): ResponseEntity<Void>

    @Operation(summary = "알림 모두 읽음 처리", description = "로그인 사용자의 읽지 않은 알림을 모두 읽음 상태로 변경합니다.")
    fun markAllRead(
        @Parameter(hidden = true) userId: Long,
    ): ResponseEntity<Void>

    @Operation(summary = "내 알림 설정 조회", description = "채팅, 모집 마감, 소셜 활동, 마케팅 및 방해 금지 시간대 설정을 반환합니다.")
    fun getSetting(
        @Parameter(hidden = true) userId: Long,
    ): NotificationSettingResponse

    @Operation(summary = "내 알림 설정 변경", description = "전체 알림 종류와 방해 금지 사용 여부·시간·요일을 한 번에 변경합니다.")
    fun updateSetting(
        @Parameter(hidden = true) userId: Long,
        request: UpdateNotificationSettingRequest,
    ): NotificationSettingResponse

    @Operation(summary = "채팅방별 알림 설정 조회", description = "별도 설정이 없으면 기본값은 켜짐입니다.")
    fun getChatRoomSetting(
        @Parameter(hidden = true) userId: Long,
        roomId: Long,
    ): ChatRoomNotificationSettingResponse

    @Operation(summary = "채팅방별 알림 켜기·끄기", description = "지정한 채팅방의 알림 수신 여부를 저장합니다. 별도 설정이 없으면 기본값은 켜짐입니다.")
    fun updateChatRoomSetting(
        @Parameter(hidden = true) userId: Long,
        roomId: Long,
        request: UpdateChatRoomNotificationSettingRequest,
    ): ChatRoomNotificationSettingResponse
}
