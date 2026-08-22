package kr.hanchae.moyeotrip.controller.notification.response

import io.swagger.v3.oas.annotations.media.Schema
import kr.hanchae.moyeotrip.entity.notification.NotificationType
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime

@Schema(description = "내 알림 목록 커서 조회 응답")
data class NotificationPageResponse(
    @field:Schema(description = "조회한 알림 목록")
    val notifications: List<NotificationResponse>,
    @field:Schema(description = "다음 페이지 조회에 사용할 마지막 알림 ID. 다음 페이지가 없으면 null", example = "95", nullable = true)
    val nextLastId: Long?,
    @field:Schema(description = "다음 페이지 존재 여부", example = "true")
    val hasNext: Boolean,
    @field:Schema(description = "전체 읽지 않은 알림 수", example = "4")
    val unreadCount: Long,
)

@Schema(description = "알림 한 건의 상세 정보")
data class NotificationResponse(
    @field:Schema(description = "알림 ID", example = "101")
    val notificationId: Long,
    @field:Schema(description = "알림 유형", example = "FEED_LIKED")
    val type: NotificationType,
    @field:Schema(description = "알림에 표시할 내용", example = "따스한 사슴 3492님이 회원님의 피드를 좋아합니다.")
    val content: String,
    @field:Schema(description = "관련 채팅방 ID. 채팅방과 무관한 알림이면 null", example = "101", nullable = true)
    val chatRoomId: Long?,
    @field:Schema(description = "알림 유형별 관련 리소스 ID(피드, 메시지, 친구 요청 등)", example = "45")
    val referenceId: Long,
    @field:Schema(description = "읽음 여부", example = "false")
    val read: Boolean,
    @field:Schema(description = "알림 생성 일시", example = "2026-09-15T20:00:00")
    val createdAt: LocalDateTime,
)

@Schema(description = "방해 금지 상세 설정")
data class NotificationSettingResponse(
    @field:Schema(description = "방해 금지 시간대 사용 여부", example = "true")
    val doNotDisturbEnabled: Boolean,
    @field:Schema(description = "방해 금지 시작 시각. 미설정이면 null", example = "22:30", nullable = true)
    val doNotDisturbStartTime: LocalTime?,
    @field:Schema(description = "방해 금지 종료 시각. 미설정이면 null", example = "07:00", nullable = true)
    val doNotDisturbEndTime: LocalTime?,
    @field:Schema(description = "방해 금지를 적용할 요일 목록")
    val doNotDisturbDays: Set<DayOfWeek>,
) {
    companion object {
        fun default(): NotificationSettingResponse =
            NotificationSettingResponse(
                doNotDisturbEnabled = false,
                doNotDisturbStartTime = null,
                doNotDisturbEndTime = null,
                doNotDisturbDays = emptySet(),
            )
    }
}

@Schema(description = "채팅방별 알림 수신 설정")
data class ChatRoomNotificationSettingResponse(
    @field:Schema(description = "채팅방 ID", example = "101")
    val roomId: Long,
    @field:Schema(description = "해당 채팅방 알림 수신 여부", example = "true")
    val enabled: Boolean,
)
